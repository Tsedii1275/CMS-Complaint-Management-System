package com.dashenbank.cms.controller;

import com.dashenbank.cms.model.CustomerFeedback;
import com.dashenbank.cms.model.ComplaintSlaMetrics;
import com.dashenbank.cms.repository.CustomerFeedbackRepository;
import com.dashenbank.cms.repository.ComplaintSlaMetricsRepository;
import com.dashenbank.cms.service.AuditService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.dashenbank.cms.exception.FeedbackTokenException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class CustomerFeedbackController {

    private static final Logger log = LoggerFactory.getLogger(CustomerFeedbackController.class);

    private static final String KEY_CSAT_SCORE = "csatScore";
    private static final String KEY_NPS_SCORE = "npsScore";
    private static final String KEY_NPS_COMMENT = "npsComment";
    private static final String KEY_CES_SCORE = "cesScore";
    private static final String KEY_CES_COMMENT = "cesComment";
    private static final String KEY_CUSTOMER = "customer";
    private static final String KEY_LABEL = "label";
    private static final String KEY_COUNT = "count";
    private static final String KEY_CUSTOMER_FEEDBACK_COMMENT = "customerFeedbackComment";
    private static final String KEY_SYSTEM = "system";
    private static final String KEY_SUCCESS = "success";
    private static final String KEY_MESSAGE = "message";
    private static final String ACTOR_TEAM_LEADER = "team_leader";
    private static final String LANG_ENGLISH = "english";
    private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();

    private final TaskService taskService;
    private final CustomerFeedbackRepository feedbackRepository;
    private final ComplaintSlaMetricsRepository slaMetricsRepository;
    private final AuditService auditService;

    @Autowired(required = false)
    private com.dashenbank.cms.service.SlaTrackingService slaTrackingService;

    public CustomerFeedbackController(TaskService taskService,
            CustomerFeedbackRepository feedbackRepository,
            ComplaintSlaMetricsRepository slaMetricsRepository,
            AuditService auditService) {
        this.taskService = taskService;
        this.feedbackRepository = feedbackRepository;
        this.slaMetricsRepository = slaMetricsRepository;
        this.auditService = auditService;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castToMap(Object obj) {
        if (obj instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private String dbcIdFromMetrics(Optional<ComplaintSlaMetrics> metricsOpt) {
        if (metricsOpt.isPresent() && metricsOpt.get().getComplaintId() != null
                && metricsOpt.get().getComplaintId().startsWith("DBC-")) {
            return metricsOpt.get().getComplaintId();
        }
        return null;
    }

    private String resolveDbcTicketId(String currentTicketNumber, String processInstanceId) {
        if (currentTicketNumber != null && currentTicketNumber.startsWith("DBC-")) {
            return currentTicketNumber;
        }
        if (processInstanceId != null && !processInstanceId.isBlank()) {
            try {
                String dbc = dbcIdFromMetrics(slaMetricsRepository.findByProcessInstanceId(processInstanceId));
                if (dbc != null) {
                    return dbc;
                }
            } catch (Exception e) {
                log.debug("Could not resolve DBC ticket by process instance {}: {}", processInstanceId, e.getMessage());
            }
        }
        if (currentTicketNumber != null && !currentTicketNumber.isBlank()) {
            try {
                String dbc = dbcIdFromMetrics(slaMetricsRepository.findByGeneralTicketId(currentTicketNumber));
                if (dbc != null) {
                    return dbc;
                }
            } catch (Exception e) {
                log.debug("Could not resolve DBC ticket by general ticket {}: {}", currentTicketNumber, e.getMessage());
            }
        }
        return currentTicketNumber;
    }

    @GetMapping("/customer-feedback/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestParam(required = false) String token) {
        if (token == null || token.isBlank()) {
            throw new FeedbackTokenException("FEEDBACK_TOKEN_MISSING", "Survey security token is required.",
                    HttpStatus.BAD_REQUEST);
        }

        var feedbackOpt = feedbackRepository.findBySecureToken(token);
        if (feedbackOpt.isEmpty()) {
            throw new FeedbackTokenException("FEEDBACK_TOKEN_NOT_FOUND",
                    "The requested feedback survey link was not found or is invalid.", HttpStatus.NOT_FOUND);
        }

        var feedback = feedbackOpt.get();
        if (Boolean.TRUE.equals(feedback.getTokenExpired())) {
            throw new FeedbackTokenException("FEEDBACK_TOKEN_EXPIRED",
                    "This feedback request has expired or has already been submitted.", HttpStatus.CONFLICT);
        }

        String ticketNumber = resolveDbcTicketId(feedback.getTicketNumber(), feedback.getComplaintId());
        if (!ticketNumber.equals(feedback.getTicketNumber())) {
            feedback.setTicketNumber(ticketNumber);
            try {
                feedbackRepository.save(feedback);
            } catch (Exception e) {
                log.debug("Could not persist resolved DBC ticket on token validate: {}", e.getMessage());
            }
        }

        return ResponseEntity.ok(Map.of(
                "valid", true,
                "ticketNumber", ticketNumber != null ? ticketNumber : "",
                "complaintId", feedback.getComplaintId() != null ? feedback.getComplaintId() : "",
                "preferredLanguage",
                feedback.getPreferredLanguage() != null ? feedback.getPreferredLanguage() : LANG_ENGLISH));
    }

    @PostMapping("/customer-feedback")
    public ResponseEntity<Map<String, Object>> submitFeedback(@RequestBody Map<String, Object> body) {
        if (body == null) {
            throw new FeedbackTokenException("INVALID_REQUEST_PAYLOAD", "Request body cannot be null.",
                    HttpStatus.BAD_REQUEST);
        }

        String token = (String) body.get("token");
        if (token == null || token.isBlank()) {
            throw new FeedbackTokenException("FEEDBACK_TOKEN_MISSING", "Survey security token is required.",
                    HttpStatus.BAD_REQUEST);
        }

        var feedbackOpt = feedbackRepository.findBySecureToken(token);
        if (feedbackOpt.isEmpty()) {
            throw new FeedbackTokenException("FEEDBACK_TOKEN_NOT_FOUND",
                    "The requested feedback survey link was not found or is invalid.", HttpStatus.NOT_FOUND);
        }

        CustomerFeedback feedback = feedbackOpt.get();
        if (Boolean.TRUE.equals(feedback.getTokenExpired())) {
            throw new FeedbackTokenException("FEEDBACK_TOKEN_EXPIRED",
                    "This feedback request has expired or has already been submitted.", HttpStatus.CONFLICT);
        }

        Object satisfiedObj = body.get("satisfied");
        if (satisfiedObj == null) {
            throw new FeedbackTokenException("INVALID_REQUEST_PAYLOAD", "Resolution satisfaction response is required.",
                    HttpStatus.BAD_REQUEST);
        }

        boolean satisfied = Boolean.parseBoolean(satisfiedObj.toString());

        Integer csatScore = parseInteger(body.get(KEY_CSAT_SCORE));
        Integer npsScore = parseInteger(body.get(KEY_NPS_SCORE));
        String npsComment = (String) body.getOrDefault(KEY_NPS_COMMENT, "");
        Integer cesScore = parseInteger(body.get(KEY_CES_SCORE));
        String cesComment = (String) body.getOrDefault(KEY_CES_COMMENT, "");
        String additionalComments = (String) body.getOrDefault("additionalComments", "");
        String resolutionSpeedRating = (String) body.getOrDefault("resolutionSpeedRating", "");
        String easeRating = (String) body.getOrDefault("easeRating", "");

        String ticketId = resolveDbcTicketId(feedback.getTicketNumber(), feedback.getComplaintId());
        String processInstanceId = feedback.getComplaintId();

        if (ticketId != null && !ticketId.equals(feedback.getTicketNumber())) {
            feedback.setTicketNumber(ticketId);
        }

        linkSlaDetailsToFeedback(feedback, processInstanceId, ticketId);

        feedback.setResolutionSpeedRating(resolutionSpeedRating);
        feedback.setEaseRating(easeRating);

        if (satisfied) {
            applyResolvedSurvey(feedback,
                    new ScoreUpdate(true, csatScore, npsScore, npsComment, cesScore, cesComment, additionalComments),
                    ticketId, processInstanceId);
        } else {
            applyUnresolvedSurvey(feedback, additionalComments, ticketId, processInstanceId);
        }

        completeResolutionTaskIfPresent(ticketId, processInstanceId, satisfied, additionalComments, csatScore, npsScore);
        feedbackRepository.save(feedback);
        persistClosedOverallStatus(processInstanceId, ticketId);

        Map<String, Object> dynamicStats = loadFeedbackStatsQuietly();
        Map<String, Object> response = new HashMap<>();
        response.put(KEY_SUCCESS, true);
        response.put("reopened", false);
        response.put("stats", dynamicStats);
        response.put(KEY_MESSAGE, satisfied ? "Thank you! Your case has been successfully closed."
                : "Thank you! Your response has been submitted. Our Customer Care Leadership team will follow up on your issue.");

        return ResponseEntity.ok(response);
    }

    private void linkSlaDetailsToFeedback(CustomerFeedback feedback, String processInstanceId, String ticketId) {
        try {
            var metricsOpt = slaMetricsRepository.findByProcessInstanceId(processInstanceId);
            if (metricsOpt.isEmpty() && ticketId != null) {
                metricsOpt = slaMetricsRepository.findByComplaintId(ticketId);
            }
            if (metricsOpt.isPresent()) {
                var m = metricsOpt.get();
                if (m.getDepartment() != null) {
                    feedback.setDepartment(m.getDepartment());
                }
                if (m.getStaffHandling() != null) {
                    feedback.setEmployeeId(m.getStaffHandling());
                } else if (m.getManager() != null) {
                    feedback.setEmployeeId(m.getManager());
                }
            }
        } catch (Exception e) {
            log.error("Failed to link SLA metrics details to feedback: {}", e.getMessage());
        }
    }

    private void applyUnresolvedSurvey(CustomerFeedback feedback, String additionalComments, String ticketId,
            String processInstanceId) {
        feedback.setResolutionConfirmed(false);
        feedback.setCustomerSatisfactionStatus("Complaint Not Resolved");
        feedback.setFollowupStatus("PENDING");
        feedback.setAdditionalComments(additionalComments);
        feedback.setSubmittedAt(LocalDateTime.now(SYSTEM_ZONE));
        feedback.setFeedbackSubmittedAt(LocalDateTime.now(SYSTEM_ZONE));
        feedback.setTokenExpired(true);
        feedback.setReopenedCase(false);

        sendUnresolvedHighPriorityAlert(ticketId);

        try {
            auditService.log(ticketId, processInstanceId, null, "UNRESOLVED_COMPLAINT_FEEDBACK", KEY_SYSTEM, KEY_SYSTEM,
                    "Customer indicated complaint was not resolved via feedback survey. In-system follow-up task routed to Customer Care Team Leader & Senior Manager queues.");
        } catch (Exception e) {
            log.warn("Could not log audit event for unresolved feedback: {}", e.getMessage());
        }
    }

    private void applyResolvedSurvey(CustomerFeedback feedback, ScoreUpdate scores, String ticketId,
            String processInstanceId) {
        updateFeedbackScores(feedback, scores);
        feedback.setCustomerSatisfactionStatus("Resolved & Satisfied");
        feedback.setReopenedCase(false);

        try {
            auditService.log(ticketId, processInstanceId, null, "CASE_CLOSED_BY_CUSTOMER", KEY_CUSTOMER,
                    KEY_CUSTOMER,
                    "Customer confirmed resolution satisfaction via feedback survey. Case closed successfully.");
        } catch (Exception e) {
            log.warn("Could not log audit event for resolved feedback: {}", e.getMessage());
        }
    }

    private void completeResolutionTaskIfPresent(String ticketId, String processInstanceId, boolean satisfied,
            String additionalComments, Integer csatScore, Integer npsScore) {
        try {
            Task targetTask = findResolutionConfirmationTask(ticketId, processInstanceId);
            if (targetTask != null) {
                Map<String, Object> variables = new HashMap<>();
                variables.put("isSatisfied", satisfied);
                variables.put(KEY_CUSTOMER_FEEDBACK_COMMENT, additionalComments);
                variables.put("customerFeedbackSubmittedAt", LocalDateTime.now(SYSTEM_ZONE).toString());
                variables.put(KEY_CSAT_SCORE, csatScore);
                variables.put(KEY_NPS_SCORE, npsScore);
                taskService.complete(targetTask.getId(), variables);
            }
        } catch (Exception e) {
            log.warn("Note: Non-critical exception completing resolution task: {}", e.getMessage());
        }
    }

    private void persistClosedOverallStatus(String processInstanceId, String ticketId) {
        if (slaTrackingService == null) {
            return;
        }
        try {
            slaTrackingService.persistOverallStatusValue(processInstanceId, ticketId,
                    com.dashenbank.cms.model.OverallComplaintStatus.CLOSED);
        } catch (Exception e) {
            log.warn("Could not persist CLOSED overall status after customer feedback: {}", e.getMessage());
        }
    }

    private Map<String, Object> loadFeedbackStatsQuietly() {
        try {
            List<CustomerFeedback> allFeedbacks = feedbackRepository.findAll();
            long totalResponses = allFeedbacks.stream().filter(f -> Boolean.TRUE.equals(f.getTokenExpired())).count();
            return calculateFeedbackStats(allFeedbacks, totalResponses);
        } catch (Exception e) {
            log.warn("Non-critical error calculating stats during feedback submit: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private void sendUnresolvedHighPriorityAlert(String ticketId) {
        log.info(
                "[IN-SYSTEM FOLLOW-UP TASK] Unresolved survey response registered for ticket #{}. No email sent per system policy.",
                ticketId);
    }

    @GetMapping("/customer-feedback/unresolved-followups")
    public ResponseEntity<List<CustomerFeedback>> getUnresolvedFollowups() {
        List<CustomerFeedback> all = feedbackRepository.findAll();
        List<CustomerFeedback> unresolved = all.stream()
                .filter(f -> Boolean.FALSE.equals(f.getResolutionConfirmed())
                        || "Complaint Not Resolved".equalsIgnoreCase(f.getCustomerSatisfactionStatus()))
                .sorted((a, b) -> {
                    LocalDateTime ta = a.getSubmittedAt() != null ? a.getSubmittedAt() : LocalDateTime.MIN;
                    LocalDateTime tb = b.getSubmittedAt() != null ? b.getSubmittedAt() : LocalDateTime.MIN;
                    return tb.compareTo(ta);
                })
                .toList();
        return ResponseEntity.ok(unresolved);
    }

    @PostMapping("/customer-feedback/unresolved-followups/{id}/start")
    public ResponseEntity<Map<String, Object>> startUnresolvedFollowup(
            @PathVariable Long id,
            @RequestParam(required = false) String username) {
        var fbOpt = feedbackRepository.findById(id);
        if (fbOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        CustomerFeedback fb = fbOpt.get();
        fb.setFollowupStatus("IN_PROGRESS");
        fb.setFollowupStartedAt(LocalDateTime.now(SYSTEM_ZONE));
        if (username != null && !username.isBlank()) {
            fb.setFollowupAssignedTo(username);
        }
        feedbackRepository.save(fb);

        try {
            auditService.log(fb.getTicketNumber(), fb.getComplaintId(), null, "START_UNRESOLVED_FOLLOWUP",
                    username != null ? username : ACTOR_TEAM_LEADER, username != null ? username : ACTOR_TEAM_LEADER,
                    "Follow-up investigation started by Customer Care leadership.");
        } catch (Exception e) {
            log.debug("Could not log START_UNRESOLVED_FOLLOWUP audit: {}", e.getMessage());
        }

        return ResponseEntity.ok(Map.of(KEY_SUCCESS, true, KEY_MESSAGE, "Follow-up status updated to IN_PROGRESS."));
    }

    @PostMapping("/customer-feedback/unresolved-followups/{id}/close")
    public ResponseEntity<Map<String, Object>> closeUnresolvedFollowup(
            @PathVariable Long id,
            @RequestParam(required = false) String username) {
        var fbOpt = feedbackRepository.findById(id);
        if (fbOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        CustomerFeedback fb = fbOpt.get();
        fb.setFollowupStatus("CLOSED");
        fb.setFollowupClosedAt(LocalDateTime.now(SYSTEM_ZONE));
        feedbackRepository.save(fb);

        try {
            auditService.log(fb.getTicketNumber(), fb.getComplaintId(), null, "CLOSE_UNRESOLVED_FOLLOWUP",
                    username != null ? username : ACTOR_TEAM_LEADER, username != null ? username : ACTOR_TEAM_LEADER,
                    "Follow-up investigation completed and closed by Customer Care leadership.");
        } catch (Exception e) {
            log.debug("Could not log CLOSE_UNRESOLVED_FOLLOWUP audit: {}", e.getMessage());
        }

        return ResponseEntity.ok(Map.of(KEY_SUCCESS, true, KEY_MESSAGE, "Follow-up status updated to CLOSED."));
    }

    @GetMapping("/customer-feedback/analytics")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> getFeedbackAnalytics() {
        List<CustomerFeedback> allFeedbacks = feedbackRepository.findAll();

        long totalSent = allFeedbacks.size();
        long totalResponses = allFeedbacks.stream().filter(f -> Boolean.TRUE.equals(f.getTokenExpired())).count();
        double responseRate = totalSent > 0 ? (totalResponses * 100.0) / totalSent : 0.0;

        Map<String, Object> loopStats = calculateFeedbackStats(allFeedbacks, totalResponses);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSent", totalSent);
        stats.put("totalResponses", totalResponses);
        stats.put("responseRate", responseRate);
        stats.putAll(loopStats);

        return ResponseEntity.ok(stats);
    }

    private Map<String, Object> calculateFeedbackStats(List<CustomerFeedback> allFeedbacks, long totalResponses) {
        List<CustomerFeedback> expired = allFeedbacks.stream()
                .filter(f -> Boolean.TRUE.equals(f.getTokenExpired()))
                .toList();

        // 1. CSAT Formula Engine: % Satisfied = (4-5 ratings) / Total valid CSAT
        // responses * 100
        List<CustomerFeedback> csatList = expired.stream()
                .filter(f -> f.getCsatScore() != null)
                .toList();
        long csatTotal = csatList.size();
        long csatSatisfiedCount = csatList.stream().filter(f -> f.getCsatScore() >= 4).count();
        double csatPercentage = csatTotal > 0 ? (csatSatisfiedCount * 100.0) / csatTotal : 0.0;
        double avgCsat = csatTotal > 0 ? csatList.stream().mapToInt(f -> f.getCsatScore()).average().orElse(0.0) : 0.0;

        // 2. NPS Formula Engine: % Promoters (9-10) minus % Detractors (0-6)
        List<CustomerFeedback> npsList = expired.stream()
                .filter(f -> f.getNpsScore() != null)
                .toList();
        long npsTotal = npsList.size();
        long promotersCount = npsList.stream().filter(f -> f.getNpsScore() >= 9).count();
        long passivesCount = npsList.stream().filter(f -> f.getNpsScore() == 7 || f.getNpsScore() == 8).count();
        long detractorsCount = npsList.stream().filter(f -> f.getNpsScore() <= 6).count();

        double npsPromotersPercent = npsTotal > 0 ? (promotersCount * 100.0) / npsTotal : 0.0;
        double npsDetractorsPercent = npsTotal > 0 ? (detractorsCount * 100.0) / npsTotal : 0.0;
        double npsPassivesPercent = npsTotal > 0 ? (passivesCount * 100.0) / npsTotal : 0.0;
        double npsScore = npsPromotersPercent - npsDetractorsPercent;
        double avgNps = npsTotal > 0 ? npsList.stream().mapToInt(f -> f.getNpsScore()).average().orElse(0.0) : 0.0;

        // 3. CES Engine
        List<CustomerFeedback> cesList = expired.stream()
                .filter(f -> f.getCesScore() != null)
                .toList();
        long cesTotal = cesList.size();
        double avgCes = cesTotal > 0 ? cesList.stream().mapToInt(f -> f.getCesScore()).average().orElse(0.0) : 0.0;

        long confirmedCount = expired.stream().filter(f -> Boolean.TRUE.equals(f.getResolutionConfirmed())).count();
        long disputedCount = expired.stream().filter(f -> Boolean.FALSE.equals(f.getResolutionConfirmed())).count();
        long reopenedCount = expired.stream().filter(f -> Boolean.TRUE.equals(f.getReopenedCase())).count();

        double confirmedRate = totalResponses > 0 ? (confirmedCount * 100.0) / totalResponses : 0.0;
        double disputeRate = totalResponses > 0 ? (disputedCount * 100.0) / totalResponses : 0.0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("csatPercentage", Math.round(csatPercentage * 10.0) / 10.0);
        stats.put("csatSatisfiedCount", csatSatisfiedCount);
        stats.put("csatTotalResponses", csatTotal);
        stats.put("avgCsat", Math.round(avgCsat * 100.0) / 100.0);

        stats.put(KEY_NPS_SCORE, Math.round(npsScore * 10.0) / 10.0);
        stats.put("npsPromotersPercent", Math.round(npsPromotersPercent * 10.0) / 10.0);
        stats.put("npsDetractorsPercent", Math.round(npsDetractorsPercent * 10.0) / 10.0);
        stats.put("npsPassivesPercent", Math.round(npsPassivesPercent * 10.0) / 10.0);
        stats.put("npsPromotersCount", promotersCount);
        stats.put("npsDetractorsCount", detractorsCount);
        stats.put("npsPassivesCount", passivesCount);
        stats.put("avgNps", Math.round(avgNps * 100.0) / 100.0);

        stats.put("avgCes", Math.round(avgCes * 100.0) / 100.0);
        stats.put("confirmationRate", Math.round(confirmedRate * 10.0) / 10.0);
        stats.put("disputeRate", Math.round(disputeRate * 10.0) / 10.0);
        stats.put("reopenedCount", reopenedCount);
        return stats;
    }

    @GetMapping("/customer-feedback/distributions")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> getFeedbackDistributions() {
        List<CustomerFeedback> allFeedbacks = feedbackRepository.findAll();

        Map<Integer, Long> csatMap = new HashMap<>();
        for (int i = 1; i <= 5; i++)
            csatMap.put(i, 0L);

        Map<Integer, Long> npsMap = new HashMap<>();
        for (int i = 0; i <= 10; i++)
            npsMap.put(i, 0L);

        Map<Integer, Long> cesMap = new HashMap<>();
        for (int i = 1; i <= 7; i++)
            cesMap.put(i, 0L);

        accumulateScoreCounts(allFeedbacks, csatMap, npsMap, cesMap);

        return ResponseEntity.ok(Map.of(
                "csat", buildDistributionList(csatMap),
                "nps", buildDistributionList(npsMap),
                "ces", buildDistributionList(cesMap)));
    }

    private void accumulateScoreCounts(List<CustomerFeedback> allFeedbacks, Map<Integer, Long> csatMap,
            Map<Integer, Long> npsMap, Map<Integer, Long> cesMap) {
        for (var f : allFeedbacks) {
            if (Boolean.TRUE.equals(f.getTokenExpired())) {
                incrementScoreCount(csatMap, f.getCsatScore());
                incrementScoreCount(npsMap, f.getNpsScore());
                incrementScoreCount(cesMap, f.getCesScore());
            }
        }
    }

    private void incrementScoreCount(Map<Integer, Long> scoreMap, Integer score) {
        if (score != null) {
            scoreMap.computeIfPresent(score, (k, v) -> v + 1);
        }
    }

    private List<Map<String, Object>> buildDistributionList(Map<Integer, Long> scoreMap) {
        return scoreMap.entrySet().stream()
                .map(e -> Map.<String, Object>of(KEY_LABEL, e.getKey().toString(), KEY_COUNT, e.getValue()))
                .toList();
    }

    @GetMapping("/customer-feedback/trends")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getFeedbackTrends() {
        List<CustomerFeedback> allFeedbacks = feedbackRepository.findAll();

        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM");

        Map<String, List<CustomerFeedback>> grouped = allFeedbacks.stream()
                .filter(f -> Boolean.TRUE.equals(f.getTokenExpired()) && f.getSubmittedAt() != null)
                .collect(Collectors.groupingBy(f -> f.getSubmittedAt().format(formatter)));

        List<Map<String, Object>> trend = grouped.entrySet().stream()
                .map(e -> {
                    String month = e.getKey();
                    List<CustomerFeedback> list = e.getValue();
                    double avgCsat = list.stream().filter(f -> f.getCsatScore() != null).mapToInt(f -> f.getCsatScore())
                            .average().orElse(0.0);
                    double avgNps = list.stream().filter(f -> f.getNpsScore() != null).mapToInt(f -> f.getNpsScore())
                            .average().orElse(0.0);
                    double avgCes = list.stream().filter(f -> f.getCesScore() != null).mapToInt(f -> f.getCesScore())
                            .average().orElse(0.0);

                    Map<String, Object> m = new HashMap<>();
                    m.put("period", month);
                    m.put("avgCsat", avgCsat);
                    m.put("avgNps", avgNps);
                    m.put("avgCes", avgCes);
                    m.put(KEY_COUNT, (long) list.size());
                    return m;
                })
                .sorted(Comparator.comparing(m -> m.get("period").toString()))
                .toList();

        return ResponseEntity.ok(trend);
    }

    @GetMapping("/customer-feedback/list")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<CustomerFeedback>> getFeedbackList(@RequestParam(required = false) String query) {
        List<CustomerFeedback> list = feedbackRepository.findAll();
        for (CustomerFeedback fb : list) {
            syncCmTicketToDbc(fb);
        }

        if (query != null && !query.isBlank()) {
            final String q = query.toLowerCase().trim();
            list = list.stream()
                    .filter(f -> (f.getTicketNumber() != null && f.getTicketNumber().toLowerCase().contains(q))
                            || (f.getComplaintId() != null && f.getComplaintId().toLowerCase().contains(q)))
                    .toList();
        }
        return ResponseEntity.ok(list);
    }

    private void syncCmTicketToDbc(CustomerFeedback fb) {
        if (fb.getTicketNumber() == null || !fb.getTicketNumber().startsWith("CM-") || fb.getComplaintId() == null) {
            return;
        }
        try {
            persistDbcTicketFromSla(fb);
        } catch (Exception e) {
            log.debug("Could not resolve DBC ticket for feedback list item {}: {}", fb.getTicketNumber(), e.getMessage());
        }
    }

    private void persistDbcTicketFromSla(CustomerFeedback fb) {
        var metricsOpt = slaMetricsRepository.findByProcessInstanceId(fb.getComplaintId());
        if (metricsOpt.isEmpty()) {
            return;
        }
        String formalDbc = metricsOpt.get().getComplaintId();
        if (formalDbc == null || !formalDbc.startsWith("DBC-")) {
            return;
        }
        fb.setTicketNumber(formalDbc);
        try {
            feedbackRepository.save(fb);
        } catch (Exception e) {
            log.debug("Could not persist DBC ticket on feedback list item: {}", e.getMessage());
        }
    }

    private Integer parseInteger(Object obj) {
        if (obj == null)
            return null;
        try {
            return Integer.parseInt(obj.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }

    private record ScoreUpdate(boolean satisfied, Integer csat, Integer nps, String npsComment, Integer ces,
            String cesComment, String additionalComments) {
    }

    private void updateFeedbackScores(CustomerFeedback feedback, ScoreUpdate scores) {
        feedback.setResolutionConfirmed(scores.satisfied());
        feedback.setCsatScore(scores.csat());
        feedback.setNpsScore(scores.nps());
        feedback.setNpsComment(scores.npsComment());
        feedback.setCesScore(scores.ces());
        feedback.setCesComment(scores.cesComment());
        feedback.setAdditionalComments(scores.additionalComments());
        feedback.setSubmittedAt(LocalDateTime.now(SYSTEM_ZONE));
        feedback.setFeedbackSubmittedAt(LocalDateTime.now(SYSTEM_ZONE));
        feedback.setTokenExpired(true);
    }

    private Task findResolutionConfirmationTask(String ticketId, String processInstanceId) {
        if (taskService == null)
            return null;
        try {
            if (processInstanceId != null && !processInstanceId.isBlank()) {
                List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
                if (!tasks.isEmpty())
                    return tasks.get(0);
            }
            if (ticketId != null && !ticketId.isBlank()) {
                List<Task> tasks = taskService.createTaskQuery().processVariableValueEquals("dbcTicketId", ticketId)
                        .list();
                if (!tasks.isEmpty())
                    return tasks.get(0);
            }
        } catch (Exception e) {
            log.error("Failed to query resolution confirmation task: {}", e.getMessage());
        }
        return null;
    }
}
