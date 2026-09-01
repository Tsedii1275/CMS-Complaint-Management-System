package com.dashenbank.cms.controller;

import com.dashenbank.cms.model.ComplaintSlaMetrics;
import com.dashenbank.cms.repository.ComplaintSlaMetricsRepository;
import com.dashenbank.cms.repository.UserRepository;
import org.flowable.engine.HistoryService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import com.dashenbank.cms.model.User;
import com.dashenbank.cms.model.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import com.dashenbank.cms.service.SlaTrackingService;

@RestController
@RequestMapping("/api/cmd/analytics")
public class CmdAnalyticsController {

    private static final String EYODA_USER = "eyoda";
    private static final String HASET_USER = "haset";
    private static final String CMD_OFFICER_ROLE = "cmd_officer";

    private final TaskService taskService;
    private final HistoryService historyService;
    private final ComplaintSlaMetricsRepository slaMetricsRepository;
    private final UserRepository userRepository;

    @Autowired
    public CmdAnalyticsController(TaskService taskService,
            HistoryService historyService,
            ComplaintSlaMetricsRepository slaMetricsRepository,
            UserRepository userRepository) {
        this.taskService = taskService;
        this.historyService = historyService;
        this.slaMetricsRepository = slaMetricsRepository;
        this.userRepository = userRepository;
    }

    /**
     * Team Leader Analytics: Officer Workload & SLA Metrics
     */
    @GetMapping("/team-workload")
    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER_CARE_TEAM_LEADER', 'ROLE_CUSTOMER_CARE_SENIOR_MANAGER', 'ROLE_SERVICE_QUALITY_DIRECTOR', 'ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> getTeamWorkload() {
        List<Task> activeCmdTasks = taskService.createTaskQuery().active().list();
        List<ComplaintSlaMetrics> slaMetrics = slaMetricsRepository.findAll().stream()
                .filter(SlaTrackingService::isClassifiedComplaint)
                .toList();

        Map<String, Long> tasksByOfficer = new LinkedHashMap<>();
        Map<String, Long> pendingByOfficer = new LinkedHashMap<>();
        Map<String, Long> breachedByOfficer = new LinkedHashMap<>();

        for (Task task : activeCmdTasks) {
            String rawAssignee = task.getAssignee();
            String officerName = resolveOfficerName(rawAssignee);
            tasksByOfficer.put(officerName, tasksByOfficer.getOrDefault(officerName, 0L) + 1);
            pendingByOfficer.put(officerName, pendingByOfficer.getOrDefault(officerName, 0L) + 1);
        }

        for (ComplaintSlaMetrics metric : slaMetrics) {
            String rawOfficer = metric.getAssignedUserId() != null ? String.valueOf(metric.getAssignedUserId()) : null;
            String officerName = resolveOfficerName(rawOfficer);
            if (Boolean.TRUE.equals(metric.getBreached())) {
                breachedByOfficer.put(officerName, breachedByOfficer.getOrDefault(officerName, 0L) + 1);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("totalActiveTasks", activeCmdTasks.size());
        response.put("tasksByOfficer", tasksByOfficer);
        response.put("pendingByOfficer", pendingByOfficer);
        response.put("breachedByOfficer", breachedByOfficer);
        response.put("totalSlaRecords", slaMetrics.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Comprehensive Officer Performance Metrics
     */
    @GetMapping("/officer-performance")
    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER_CARE_TEAM_LEADER', 'ROLE_CUSTOMER_CARE_SENIOR_MANAGER', 'ROLE_SERVICE_QUALITY_DIRECTOR', 'ROLE_ADMIN')")
    @SuppressWarnings("java:S3776")
    public ResponseEntity<List<Map<String, Object>>> getOfficerPerformanceMetrics() {
        List<Task> activeTasks = taskService.createTaskQuery().active().list();
        List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery().finished().list();
        List<ComplaintSlaMetrics> slaMetrics = slaMetricsRepository.findAll().stream()
                .filter(SlaTrackingService::isClassifiedComplaint)
                .toList();
        List<User> officers = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ROLE_CUSTOMER_CARE_OFFICER)
                .toList();

        List<Map<String, Object>> result = new ArrayList<>();

        Set<String> officerUsernames = new LinkedHashSet<>();
        officers.forEach(u -> officerUsernames.add(u.getUsername()));
        if (officerUsernames.isEmpty()) {
            officerUsernames.add(EYODA_USER);
            officerUsernames.add(HASET_USER);
        }

        // Exclude complaints resolved via First Contact Resolution (FCR) as they are
        // handled at intake
        Set<String> fcrProcessInstanceIds = slaMetrics.stream()
                .filter(m -> Boolean.TRUE.equals(m.getFcrStatus()))
                .map(ComplaintSlaMetrics::getProcessInstanceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (String uname : officerUsernames) {
            String fullName = resolveOfficerName(uname);

            long activeCount = activeTasks.stream()
                    .filter(t -> {
                        String piId = t.getProcessInstanceId();
                        if (piId != null && fcrProcessInstanceIds.contains(piId))
                            return false;
                        String assignee = t.getAssignee();
                        return uname.equalsIgnoreCase(assignee)
                                || (EYODA_USER.equalsIgnoreCase(uname) && CMD_OFFICER_ROLE.equalsIgnoreCase(assignee));
                    }).count();

            // Count all workflow stage tasks claimed and performed by officer
            long completedCount = historicTasks.stream()
                    .filter(t -> {
                        String piId = t.getProcessInstanceId();
                        if (piId != null && fcrProcessInstanceIds.contains(piId))
                            return false;
                        String assignee = t.getAssignee();
                        return uname.equalsIgnoreCase(assignee)
                                || (EYODA_USER.equalsIgnoreCase(uname) && CMD_OFFICER_ROLE.equalsIgnoreCase(assignee));
                    })
                    .count();

            long slaBreached = slaMetrics.stream().filter(m -> {
                String assignedUser = m.getAssignedUserId() != null ? String.valueOf(m.getAssignedUserId()) : "";
                boolean matchesUser = uname.equalsIgnoreCase(assignedUser)
                        || (EYODA_USER.equalsIgnoreCase(uname) && CMD_OFFICER_ROLE.equalsIgnoreCase(assignedUser));
                return matchesUser && Boolean.TRUE.equals(m.getBreached());
            }).count();

            long totalAssigned = activeCount + completedCount;
            long slaMet = Math.max(0, completedCount - slaBreached);
            double slaComplianceRate = completedCount > 0 ? (double) slaMet / completedCount * 100.0 : 100.0;

            String workloadStatus = "Low";
            if (activeCount >= 6) {
                workloadStatus = "High";
            } else if (activeCount >= 3) {
                workloadStatus = "Medium";
            }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("officerName", fullName);
            row.put("username", uname);
            row.put("totalAssignedTasks", totalAssigned);
            row.put("completedTasks", completedCount);
            row.put("activeTasks", activeCount);
            row.put("slaMetTasks", slaMet);
            row.put("slaBreachedTasks", slaBreached);
            row.put("slaComplianceRate", Math.round(slaComplianceRate * 10.0) / 10.0);
            row.put("workloadStatus", workloadStatus);
            result.add(row);
        }

        return ResponseEntity.ok(result);
    }

    private String resolveOfficerName(String raw) {
        if (raw == null || raw.isBlank() || "Unassigned".equalsIgnoreCase(raw)) {
            return "Unassigned Queue";
        }
        if (EYODA_USER.equalsIgnoreCase(raw) || CMD_OFFICER_ROLE.equalsIgnoreCase(raw)) {
            return "Eyoda";
        }
        if (HASET_USER.equalsIgnoreCase(raw) || "cc_officer".equalsIgnoreCase(raw)) {
            return "Haset";
        }
        var userOpt = userRepository.findByUsernameIgnoreCase(raw);
        if (userOpt.isPresent()) {
            var u = userOpt.get();
            return u.getFullName() != null && !u.getFullName().isBlank() ? u.getFullName() : u.getUsername();
        }
        return raw;
    }

    /**
     * Senior Manager Analytics: Department Performance & Trends
     */
    @GetMapping("/department-performance")
    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER_CARE_SENIOR_MANAGER', 'ROLE_SERVICE_QUALITY_DIRECTOR', 'ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> getDepartmentPerformance() {
        List<ComplaintSlaMetrics> allMetrics = slaMetricsRepository.findAll().stream()
                .filter(SlaTrackingService::isClassifiedComplaint)
                .toList();

        Set<String> cmdUsernames = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ROLE_CUSTOMER_CARE_OFFICER
                        || u.getRole() == Role.ROLE_CUSTOMER_CARE_TEAM_LEADER
                        || u.getRole() == Role.ROLE_CUSTOMER_CARE_SENIOR_MANAGER
                        || u.getRole() == Role.ROLE_SERVICE_QUALITY_DIRECTOR)
                .map(User::getUsername)
                .collect(Collectors.toSet());
        cmdUsernames.add(EYODA_USER);
        cmdUsernames.add(HASET_USER);
        cmdUsernames.add("initiator");
        cmdUsernames.add(CMD_OFFICER_ROLE);
        cmdUsernames.add("cc_officer");

        List<ComplaintSlaMetrics> deptMetrics = allMetrics.stream()
                .filter(m -> {
                    String assignee = m.getAssignedUserId() != null ? String.valueOf(m.getAssignedUserId()) : "";
                    String dept = m.getDepartment() != null ? m.getDepartment() : "";
                    String stage = m.getCurrentStage() != null ? m.getCurrentStage() : "";
                    return cmdUsernames.contains(assignee.toLowerCase()) ||
                            dept.equalsIgnoreCase("Customer Care") ||
                            dept.equalsIgnoreCase("CMD") ||
                            stage.toUpperCase().contains("CMD") ||
                            stage.toUpperCase().contains("CUSTOMER");
                })
                .toList();

        List<ComplaintSlaMetrics> targetMetrics = deptMetrics.isEmpty() ? allMetrics : deptMetrics;

        long totalCount = targetMetrics.size();
        long breachedCount = targetMetrics.stream().filter(m -> Boolean.TRUE.equals(m.getBreached())).count();
        long onTimeCount = totalCount - breachedCount;
        double complianceRate = totalCount > 0 ? (double) onTimeCount / totalCount * 100.0 : 100.0;

        Map<String, Long> volumeByChannel = targetMetrics.stream()
                .filter(m -> m.getChannel() != null)
                .collect(Collectors.groupingBy(ComplaintSlaMetrics::getChannel, Collectors.counting()));

        Map<String, Long> volumeByCategory = targetMetrics.stream()
                .filter(m -> m.getComplaintCategory() != null)
                .collect(Collectors.groupingBy(ComplaintSlaMetrics::getComplaintCategory, Collectors.counting()));

        Map<String, Object> response = new HashMap<>();
        response.put("totalComplaintVolume", totalCount);
        response.put("onTimeCount", onTimeCount);
        response.put("breachedCount", breachedCount);
        response.put("slaComplianceRate", Math.round(complianceRate * 10.0) / 10.0);
        response.put("volumeByChannel", volumeByChannel);
        response.put("volumeByCategory", volumeByCategory);

        return ResponseEntity.ok(response);
    }

    /**
     * Exportable Report for Customer Care Senior Manager
     */
    @GetMapping("/export/csv")
    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER_CARE_SENIOR_MANAGER', 'ROLE_SERVICE_QUALITY_DIRECTOR', 'ROLE_ADMIN')")
    @SuppressWarnings("java:S3776")
    public ResponseEntity<byte[]> exportCmdDepartmentReport() {
        List<ComplaintSlaMetrics> metrics = slaMetricsRepository.findAll().stream()
                .filter(SlaTrackingService::isClassifiedComplaint)
                .toList();
        StringBuilder csv = new StringBuilder();
        csv.append(
                "Ticket ID,Customer Name,Channel,Category,Branch,District,Priority,SLA Status,Breached,Assigned Officer,Status,Classification")
                .append(System.lineSeparator());

        for (ComplaintSlaMetrics m : metrics) {
            csv.append(String.format("%s,\"%s\",%s,%s,\"%s\",\"%s\",%s,%s,%s,\"%s\",%s,%s%n",
                    m.getComplaintId() != null ? m.getComplaintId() : "",
                    m.getCustomerName() != null ? m.getCustomerName().replace("\"", "'") : "Valued Customer",
                    m.getChannel() != null ? m.getChannel() : "branch",
                    m.getComplaintCategory() != null ? m.getComplaintCategory() : "Customer Service Issues",
                    m.getBranch() != null ? m.getBranch() : "Main Branch",
                    m.getDistrict() != null ? m.getDistrict() : "Central District",
                    m.getPriority() != null ? m.getPriority() : "Normal",
                    m.getSlaStatus() != null ? m.getSlaStatus() : "WITHIN_SLA",
                    Boolean.TRUE.equals(m.getBreached()) ? "YES" : "NO",
                    m.getAssignedUserId() != null ? m.getAssignedUserId() : "Unassigned",
                    m.getStatus() != null ? m.getStatus() : "DECLINED",
                    m.getClassification() != null ? m.getClassification() : "DECLINED"));
        }

        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "cmd_department_performance_report.csv");

        return ResponseEntity.ok().headers(headers).body(bytes);
    }
}
