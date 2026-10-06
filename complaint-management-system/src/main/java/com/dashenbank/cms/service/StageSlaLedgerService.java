package com.dashenbank.cms.service;

import com.dashenbank.cms.model.ComplaintSlaMetrics;
import com.dashenbank.cms.model.SlaBreachRecord;
import com.dashenbank.cms.model.StageSlaEvent;
import com.dashenbank.cms.model.TaskTimeTracking;
import com.dashenbank.cms.repository.ComplaintSlaMetricsRepository;
import com.dashenbank.cms.repository.SlaBreachRecordRepository;
import com.dashenbank.cms.repository.StageSlaEventRepository;
import org.flowable.engine.HistoryService;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class StageSlaLedgerService {

    private static final Logger log = LoggerFactory.getLogger(StageSlaLedgerService.class);
    private static final ZoneId SYSTEM_ZONE = ZoneId.of("Africa/Addis_Ababa");
    private static final String INITIATOR = "initiator";

    private final StageSlaEventRepository eventRepository;
    private final ComplaintSlaMetricsRepository slaMetricsRepository;
    private final SlaBreachRecordRepository breachRecordRepository;
    private final SlaTrackingService slaTrackingService;
    private final BusinessHoursService businessHoursService;
    private final HistoryService historyService;

    public StageSlaLedgerService(StageSlaEventRepository eventRepository,
            ComplaintSlaMetricsRepository slaMetricsRepository,
            SlaBreachRecordRepository breachRecordRepository,
            SlaTrackingService slaTrackingService,
            BusinessHoursService businessHoursService,
            ObjectProvider<HistoryService> historyServiceProvider) {
        this.eventRepository = eventRepository;
        this.slaMetricsRepository = slaMetricsRepository;
        this.breachRecordRepository = breachRecordRepository;
        this.slaTrackingService = slaTrackingService;
        this.businessHoursService = businessHoursService;
        this.historyService = historyServiceProvider.getIfAvailable();
    }

    @Transactional
    public StageSlaEvent onTaskCreated(String processInstanceId, String complaintId, String taskId,
            String taskDefinitionKey, String assignedUser) {
        if (!StringUtils.hasText(taskId)) {
            return null;
        }
        Optional<StageSlaEvent> existing = eventRepository.findByTaskId(taskId);
        if (existing.isPresent()) {
            StageSlaEvent event = existing.get();
            if (StringUtils.hasText(assignedUser) && !INITIATOR.equalsIgnoreCase(assignedUser)) {
                event.setAssignedUser(assignedUser);
            }
            if (StringUtils.hasText(complaintId) && !taskId.equals(event.getComplaintId())) {
                event.setComplaintId(complaintId);
            }
            return eventRepository.save(event);
        }
        ComplaintSlaMetrics metrics = findMetrics(processInstanceId, complaintId);
        String ticket = firstNonBlank(complaintId,
                metrics != null ? metrics.getComplaintId() : null,
                processInstanceId);
        String canonical = SlaAlertScope.canonicalStageOf(taskDefinitionKey);
        if (canonical == null) {
            canonical = taskDefinitionKey != null ? taskDefinitionKey : "UNKNOWN";
        }
        LocalDateTime now = LocalDateTime.now(SYSTEM_ZONE);
        Integer allowed = slaTrackingService.calculateStageSlaMinutes(taskDefinitionKey,
                metrics != null ? metrics.getPriority() : "GENERAL",
                metrics != null ? metrics.getInvestigationType() : null);
        StageSlaEvent event = StageSlaEvent.builder()
                .complaintId(ticket)
                .processInstanceId(processInstanceId)
                .taskId(taskId)
                .taskDefinitionKey(taskDefinitionKey)
                .canonicalStage(canonical)
                .assignedUser(blankToNull(assignedUser))
                .startedAt(now)
                .allowedMinutes(allowed)
                .elapsedBusinessMinutes(0)
                .status(StageSlaEvent.STATUS_IN_PROGRESS)
                .breachRecorded(false)
                .build();
        return eventRepository.save(event);
    }

    @Transactional
    public StageSlaEvent onTaskCompleted(String taskId, String completedBy) {
        if (!StringUtils.hasText(taskId)) {
            return null;
        }
        Optional<StageSlaEvent> existing = eventRepository.findByTaskId(taskId);
        if (existing.isEmpty()) {
            return null;
        }
        StageSlaEvent event = existing.get();
        LocalDateTime now = LocalDateTime.now(SYSTEM_ZONE);
        if (event.getCompletedAt() == null) {
            event.setCompletedAt(now);
        }
        if (StringUtils.hasText(completedBy) && !INITIATOR.equalsIgnoreCase(completedBy)) {
            event.setAssignedUser(completedBy);
        }
        evaluateEvent(event, now);
        if (!StageSlaEvent.STATUS_BREACHED.equalsIgnoreCase(event.getStatus())) {
            event.setStatus(StageSlaEvent.STATUS_MET);
        }
        return eventRepository.save(event);
    }

    @Transactional
    public void evaluateOpenEvents() {
        List<StageSlaEvent> open = eventRepository.findByStatusIn(List.of(
                StageSlaEvent.STATUS_IN_PROGRESS, StageSlaEvent.STATUS_APPROACHING, StageSlaEvent.STATUS_BREACHED));
        LocalDateTime now = LocalDateTime.now(SYSTEM_ZONE);
        for (StageSlaEvent event : open) {
            if (event.getCompletedAt() != null) {
                continue;
            }
            try {
                evaluateEvent(event, now);
                eventRepository.save(event);
            } catch (RuntimeException e) {
                log.warn("Stage SLA evaluation failed for task {}: {}", event.getTaskId(), e.getMessage());
            }
        }
    }

    @Transactional
    public StageSlaEvent evaluateTask(String taskId) {
        Optional<StageSlaEvent> existing = eventRepository.findByTaskId(taskId);
        if (existing.isEmpty()) {
            return null;
        }
        StageSlaEvent event = existing.get();
        evaluateEvent(event, LocalDateTime.now(SYSTEM_ZONE));
        return eventRepository.save(event);
    }

    public Optional<StageSlaEvent> findByTaskId(String taskId) {
        return eventRepository.findByTaskId(taskId);
    }

    public List<StageSlaEvent> eventsForComplaint(String complaintId) {
        if (!StringUtils.hasText(complaintId)) {
            return List.of();
        }
        Optional<ComplaintSlaMetrics> metrics = resolveMetrics(complaintId);
        Map<String, StageSlaEvent> byTask = new HashMap<>();
        if (metrics.isPresent()) {
            ComplaintSlaMetrics m = metrics.get();
            if (StringUtils.hasText(m.getProcessInstanceId())) {
                for (StageSlaEvent event : eventRepository
                        .findByProcessInstanceIdOrderByStartedAtAsc(m.getProcessInstanceId())) {
                    byTask.put(event.getTaskId(), event);
                }
            }
            addEvents(byTask, eventRepository.findByComplaintIdOrderByStartedAtAsc(m.getComplaintId()));
            if (StringUtils.hasText(m.getDbcTicketId())) {
                addEvents(byTask, eventRepository.findByComplaintIdOrderByStartedAtAsc(m.getDbcTicketId()));
            }
            if (StringUtils.hasText(m.getGeneralTicketId())) {
                addEvents(byTask, eventRepository.findByComplaintIdOrderByStartedAtAsc(m.getGeneralTicketId()));
            }
        }
        addEvents(byTask, eventRepository.findByComplaintIdOrderByStartedAtAsc(complaintId.trim()));
        List<StageSlaEvent> list = new ArrayList<>(byTask.values());
        list.sort(Comparator.comparing(StageSlaEvent::getStartedAt, Comparator.nullsLast(Comparator.naturalOrder())));
        return list;
    }

    public Map<String, Object> stageKpis(String complaintId) {
        List<StageSlaEvent> events = eventsForComplaint(complaintId);
        long breached = events.stream()
                .filter(e -> StageSlaEvent.STATUS_BREACHED.equalsIgnoreCase(e.getStatus()))
                .count();
        Map<String, Object> kpis = new HashMap<>();
        kpis.put("totalStages", events.size());
        kpis.put("breachedStages", breached);
        events.stream()
                .filter(e -> e.getCompletedAt() == null)
                .reduce((first, second) -> second)
                .ifPresentOrElse(active -> {
                    kpis.put("currentStageSlaStatus", active.getStatus());
                    kpis.put("currentCanonicalStage", active.getCanonicalStage());
                    kpis.put("currentStageAllowedMinutes", active.getAllowedMinutes());
                    kpis.put("currentStageElapsedMinutes", active.getElapsedBusinessMinutes());
                }, () -> {
                    kpis.put("currentStageSlaStatus", events.isEmpty() ? null : events.get(events.size() - 1).getStatus());
                    kpis.put("currentCanonicalStage", events.isEmpty() ? null
                            : events.get(events.size() - 1).getCanonicalStage());
                });
        return kpis;
    }

    public List<TaskTimeTracking> timelineForComplaint(String complaintId) {
        List<StageSlaEvent> events = eventsForComplaint(complaintId);
        List<TaskTimeTracking> timeline = new ArrayList<>();
        for (StageSlaEvent event : events) {
            timeline.add(toTracking(event));
        }
        return timeline;
    }

    @Transactional
    public int backfillFromFlowableHistory() {
        if (historyService == null) {
            return 0;
        }
        List<HistoricTaskInstance> historic = historyService.createHistoricTaskInstanceQuery().list();
        int created = 0;
        for (HistoricTaskInstance ht : historic) {
            if (!StringUtils.hasText(ht.getId()) || eventRepository.findByTaskId(ht.getId()).isPresent()) {
                continue;
            }
            ComplaintSlaMetrics metrics = findMetrics(ht.getProcessInstanceId(), null);
            String canonical = SlaAlertScope.canonicalStageOf(ht.getTaskDefinitionKey());
            if (canonical == null) {
                canonical = ht.getTaskDefinitionKey() != null ? ht.getTaskDefinitionKey() : "UNKNOWN";
            }
            LocalDateTime start = toLocal(ht.getStartTime());
            LocalDateTime end = toLocal(ht.getEndTime());
            Integer allowed = slaTrackingService.calculateStageSlaMinutes(ht.getTaskDefinitionKey(),
                    metrics != null ? metrics.getPriority() : "GENERAL",
                    metrics != null ? metrics.getInvestigationType() : null);
            long elapsed = 0L;
            if (start != null) {
                elapsed = businessHoursService.calculateElapsedBusinessMinutes(start, end != null ? end : LocalDateTime.now(SYSTEM_ZONE));
            }
            String status = StageSlaEvent.STATUS_IN_PROGRESS;
            if (end != null) {
                status = (allowed != null && elapsed > allowed) ? StageSlaEvent.STATUS_BREACHED : StageSlaEvent.STATUS_MET;
            } else if (allowed != null && elapsed > allowed) {
                status = StageSlaEvent.STATUS_BREACHED;
            } else if (allowed != null && elapsed >= (long) (allowed * 0.80)) {
                status = StageSlaEvent.STATUS_APPROACHING;
            }
            StageSlaEvent event = StageSlaEvent.builder()
                    .complaintId(metrics != null ? firstNonBlank(metrics.getComplaintId(), metrics.getDbcTicketId(),
                            ht.getProcessInstanceId()) : ht.getProcessInstanceId())
                    .processInstanceId(ht.getProcessInstanceId())
                    .taskId(ht.getId())
                    .taskDefinitionKey(ht.getTaskDefinitionKey())
                    .canonicalStage(canonical)
                    .assignedUser(ht.getAssignee())
                    .startedAt(start)
                    .completedAt(end)
                    .allowedMinutes(allowed)
                    .elapsedBusinessMinutes((int) elapsed)
                    .status(status)
                    .breachedAt(StageSlaEvent.STATUS_BREACHED.equals(status) ? (end != null ? end : LocalDateTime.now(SYSTEM_ZONE)) : null)
                    .breachRecorded(false)
                    .build();
            eventRepository.save(event);
            if (StageSlaEvent.STATUS_BREACHED.equals(status)) {
                persistStageBreach(event, metrics);
            }
            created++;
        }
        return created;
    }

    public LocalDateTime dueAt(StageSlaEvent event) {
        if (event == null || event.getStartedAt() == null || event.getAllowedMinutes() == null) {
            return null;
        }
        return businessHoursService.addBusinessMinutes(event.getStartedAt(), event.getAllowedMinutes());
    }

    private void evaluateEvent(StageSlaEvent event, LocalDateTime now) {
        if (event.getStartedAt() == null) {
            event.setStartedAt(now);
        }
        if (event.getCompletedAt() == null && StringUtils.hasText(event.getTaskDefinitionKey())) {
            ComplaintSlaMetrics metrics = findMetrics(event.getProcessInstanceId(), event.getComplaintId());
            Integer fromMatrix = slaTrackingService.calculateStageSlaMinutes(event.getTaskDefinitionKey(),
                    metrics != null ? metrics.getPriority() : "GENERAL",
                    metrics != null ? metrics.getInvestigationType() : null);
            if (fromMatrix != null) {
                event.setAllowedMinutes(fromMatrix);
            }
        } else if (event.getAllowedMinutes() == null && StringUtils.hasText(event.getTaskDefinitionKey())) {
            ComplaintSlaMetrics metrics = findMetrics(event.getProcessInstanceId(), event.getComplaintId());
            event.setAllowedMinutes(slaTrackingService.calculateStageSlaMinutes(event.getTaskDefinitionKey(),
                    metrics != null ? metrics.getPriority() : "GENERAL",
                    metrics != null ? metrics.getInvestigationType() : null));
        }
        LocalDateTime end = event.getCompletedAt() != null ? event.getCompletedAt() : now;
        long elapsed = businessHoursService.calculateElapsedBusinessMinutes(event.getStartedAt(), end);
        event.setElapsedBusinessMinutes((int) elapsed);
        Integer allowed = event.getAllowedMinutes();
        if (allowed == null) {
            return;
        }
        boolean alreadyBreached = StageSlaEvent.STATUS_BREACHED.equalsIgnoreCase(event.getStatus());
        if (elapsed > allowed) {
            event.setStatus(StageSlaEvent.STATUS_BREACHED);
            if (event.getBreachedAt() == null) {
                event.setBreachedAt(now);
            }
            if (!Boolean.TRUE.equals(event.getBreachRecorded())) {
                persistStageBreach(event, findMetrics(event.getProcessInstanceId(), event.getComplaintId()));
            }
        } else if (event.getCompletedAt() == null && !alreadyBreached
                && elapsed >= (long) (allowed * 0.80)) {
            event.setStatus(StageSlaEvent.STATUS_APPROACHING);
        } else if (event.getCompletedAt() == null && !alreadyBreached) {
            event.setStatus(StageSlaEvent.STATUS_IN_PROGRESS);
        }
    }

    private void persistStageBreach(StageSlaEvent event, ComplaintSlaMetrics metrics) {
        if (event.getTaskId() != null && breachRecordRepository.existsByTaskId(event.getTaskId())) {
            event.setBreachRecorded(true);
            return;
        }
        String complaintId = firstNonBlank(event.getComplaintId(),
                metrics != null ? metrics.getComplaintId() : null, event.getProcessInstanceId());
        int elapsed = event.getElapsedBusinessMinutes() != null ? event.getElapsedBusinessMinutes() : 0;
        int allowed = event.getAllowedMinutes() != null ? event.getAllowedMinutes() : 0;
        long over = Math.max(1L, elapsed - allowed);
        String unit = metrics != null
                ? firstNonBlank(metrics.getDepartment(), metrics.getBranch(), "Responsible unit")
                : "Responsible unit";
        SlaBreachRecord breach = SlaBreachRecord.builder()
                .complaintId(complaintId)
                .processInstanceId(event.getProcessInstanceId())
                .taskId(event.getTaskId())
                .stageName(event.getCanonicalStage())
                .breachReason("Stage SLA exceeded (" + elapsed + "/" + allowed + " mins)")
                .breachDurationMinutes(over)
                .responsibleWorkUnit(unit)
                .escalationActionsTaken("Stage breach recorded for " + event.getCanonicalStage())
                .build();
        breachRecordRepository.save(breach);
        event.setBreachRecorded(true);
        log.warn("Recorded stage SLA breach for ticket {} stage {} task {}", complaintId, event.getCanonicalStage(),
                event.getTaskId());
    }

    private TaskTimeTracking toTracking(StageSlaEvent event) {
        long duration = event.getElapsedBusinessMinutes() != null ? event.getElapsedBusinessMinutes() : 0L;
        String slaStatus = switch (event.getStatus() == null ? "" : event.getStatus().toUpperCase()) {
            case "BREACHED" -> "BREACHED";
            case "APPROACHING" -> "APPROACHING";
            case "MET" -> "ON_TIME";
            default -> "ON_TIME";
        };
        if (event.getCompletedAt() == null && StageSlaEvent.STATUS_IN_PROGRESS.equalsIgnoreCase(event.getStatus())) {
            slaStatus = "ON_TIME";
        }
        return TaskTimeTracking.builder()
                .id(event.getId())
                .processInstanceId(event.getProcessInstanceId())
                .complaintId(event.getComplaintId())
                .taskId(event.getTaskId())
                .taskDefinitionKey(event.getTaskDefinitionKey())
                .taskName(SlaAlertScope.stageLabel(event.getCanonicalStage()))
                .laneName(SlaAlertScope.laneFromTaskKey(event.getTaskDefinitionKey()))
                .assignedUser(event.getAssignedUser())
                .startedAt(event.getStartedAt())
                .completedAt(event.getCompletedAt())
                .durationMinutes(duration)
                .durationHours(Math.round((duration / 60.0) * 100.0) / 100.0)
                .resolutionSlaTargetMinutes(event.getAllowedMinutes())
                .resolutionSlaStatus(slaStatus)
                .resolutionBreachDurationMinutes("BREACHED".equals(slaStatus)
                        ? Math.max(0L, duration - (event.getAllowedMinutes() != null ? event.getAllowedMinutes() : 0))
                        : 0L)
                .build();
    }

    private Optional<ComplaintSlaMetrics> resolveMetrics(String complaintId) {
        String target = complaintId.trim();
        Optional<ComplaintSlaMetrics> opt = slaMetricsRepository.findByDbcTicketId(target);
        if (opt.isEmpty()) {
            opt = slaMetricsRepository.findByComplaintId(target);
        }
        if (opt.isEmpty()) {
            opt = slaMetricsRepository.findByGeneralTicketId(target);
        }
        if (opt.isEmpty()) {
            opt = slaMetricsRepository.findByProcessInstanceId(target);
        }
        return opt;
    }

    private ComplaintSlaMetrics findMetrics(String processInstanceId, String complaintId) {
        if (StringUtils.hasText(processInstanceId)) {
            Optional<ComplaintSlaMetrics> byPi = slaMetricsRepository.findByProcessInstanceId(processInstanceId);
            if (byPi.isPresent()) {
                return byPi.get();
            }
        }
        if (StringUtils.hasText(complaintId)) {
            return resolveMetrics(complaintId).orElse(null);
        }
        return null;
    }

    private static void addEvents(Map<String, StageSlaEvent> byTask, List<StageSlaEvent> events) {
        if (events == null) {
            return;
        }
        for (StageSlaEvent event : events) {
            if (event.getTaskId() != null) {
                byTask.putIfAbsent(event.getTaskId(), event);
            }
        }
    }

    private static LocalDateTime toLocal(java.util.Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(SYSTEM_ZONE).toLocalDateTime();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }
}
