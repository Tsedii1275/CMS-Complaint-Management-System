package com.dashenbank.cms.controller;

import com.dashenbank.cms.model.AuditLog;
import com.dashenbank.cms.model.ComplaintSlaMetrics;
import com.dashenbank.cms.model.TaskTimeTracking;
import com.dashenbank.cms.repository.ComplaintSlaMetricsRepository;
import com.dashenbank.cms.service.AuditService;
import com.dashenbank.cms.service.SlaStatusRules;
import com.dashenbank.cms.service.SlaTrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    @Autowired
    private AuditService auditService;

    @Autowired
    private SlaTrackingService slaTrackingService;

    @Autowired
    private ComplaintSlaMetricsRepository slaMetricsRepository;

    @Autowired
    private com.dashenbank.cms.repository.UserRepository userRepository;

    @Autowired
    private org.flowable.engine.HistoryService historyService;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuditController.class);

    private static final String COMPLAINT_ID = "complaintId";
    private static final String GENERAL_TICKET_ID = "generalTicketId";
    private static final String CREATED_AT = "createdAt";
    private static final String STATUS_CLOSED = "CLOSED";
    private static final String ACCOUNT_NUMBER = "accountNumber";
    private static final String PHONE = "phone";
    private static final String DESCRIPTION = "description";
    private static final String EVIDENCE_URL = "evidenceUrl";
    private static final String VOICE_ATTACHMENT_URL = "voiceAttachmentUrl";

    private static final class AnalyticsFilter {
        String category;
        String branch;
        String district;
        String channel;
        String status;
        String slaStatus;
        Boolean fcrStatus;
        LocalDateTime startDate;
        LocalDateTime endDate;
    }

    private String getCurrentUserRole() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null && !auth.getAuthorities().isEmpty()) {
            return auth.getAuthorities().iterator().next().getAuthority();
        }
        return "ROLE_ANONYMOUS";
    }

    private String getCurrentUsername() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            return auth.getName();
        }
        return "anonymous";
    }

    private List<ComplaintSlaMetrics> filterByRoleOwnership(List<ComplaintSlaMetrics> metrics) {
        String role = getCurrentUserRole();
        if ("ROLE_ADMIN".equals(role) || "ROLE_CUSTOMER_CARE_OFFICER".equals(role)
                || "ROLE_CHIEF_COMMITTEE".equals(role)
                || "ROLE_AUDIT_INVESTIGATION_TEAM".equals(role) || "ROLE_OPERATIONAL_AUDIT_SENIOR_MANAGER".equals(role)
                || "ROLE_OPERATIONAL_AUDIT_DIRECTOR".equals(role)) {
            return metrics;
        }

        String username = getCurrentUsername();
        var userOpt = userRepository.findByUsernameIgnoreCase(username);
        if (userOpt.isPresent()) {
            com.dashenbank.cms.model.User user = userOpt.get();
            String branch = user.getBranch();
            String department = user.getDepartment();

            if ("ROLE_DEPARTMENT_WORKUNIT".equals(role)) {
                return metrics.stream()
                        .filter(m -> {
                            boolean matchBranch = branch == null || branch.isBlank()
                                    || branch.equalsIgnoreCase(m.getBranch());
                            boolean matchDept = department == null || department.isBlank()
                                    || department.equalsIgnoreCase(m.getDepartment());
                            return matchBranch && matchDept;
                        })
                        .collect(Collectors.toList());
            }
        }
        return metrics;
    }

    @GetMapping("/logs")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<AuditLog> getLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String complaintId,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return auditService.getLogs(action, complaintId, actor, startDate, endDate);
    }

    // ─── SLA Metrics Endpoints ───

    @GetMapping("/sla/all")
    public ResponseEntity<List<ComplaintSlaMetrics>> getAllSlaMetrics() {
        List<ComplaintSlaMetrics> metrics = slaTrackingService.getAllMetrics();
        return ResponseEntity.ok(filterByRoleOwnership(metrics));
    }

    @GetMapping("/sla/other")
    public ResponseEntity<List<ComplaintSlaMetrics>> getOtherSlaMetrics() {
        List<ComplaintSlaMetrics> metrics = slaTrackingService.getOtherMetrics();
        return ResponseEntity.ok(filterByRoleOwnership(metrics));
    }

    @GetMapping("/sla/process/{processInstanceId}")
    public ResponseEntity<Map<String, Object>> getSlaByProcessInstance(@PathVariable String processInstanceId) {
        return ResponseEntity.ok(slaTrackingService.buildSlaReport(processInstanceId));
    }

    @GetMapping("/sla/complaint/{complaintId}")
    public ResponseEntity<Map<String, Object>> getSlaByComplaintId(@PathVariable String complaintId) {
        var metrics = slaTrackingService.getMetricsByComplaintId(complaintId);
        if (metrics.isEmpty()) {
            return ResponseEntity.ok(Map.of("available", false));
        }
        return ResponseEntity.ok(slaTrackingService.buildSlaReport(metrics.get().getProcessInstanceId()));
    }

    @GetMapping("/sla/tasks/{processInstanceId}")
    public ResponseEntity<List<TaskTimeTracking>> getTaskTracking(@PathVariable String processInstanceId) {
        return ResponseEntity.ok(slaTrackingService.getTaskTrackingByProcessInstanceId(processInstanceId));
    }

    // ─── Advanced Analytics & Reporting Module Endpoints ───

    private List<ComplaintSlaMetrics> loadFilteredMetrics(AnalyticsFilter filter) {
        List<ComplaintSlaMetrics> classified = slaTrackingService.getAllMetrics();
        return filterByRoleOwnership(classified.stream()
                .filter(m -> matchesAnalyticsFilter(m, filter))
                .toList());
    }

    private boolean matchesAnalyticsFilter(ComplaintSlaMetrics m, AnalyticsFilter filter) {
        if (filter == null) {
            return true;
        }
        if (!equalsIgnoreNull(filter.category, m.getComplaintCategory())) {
            return false;
        }
        if (!equalsIgnoreNull(filter.branch, m.getBranch())) {
            return false;
        }
        if (!equalsIgnoreNull(filter.district, m.getDistrict())) {
            return false;
        }
        if (!equalsIgnoreNull(filter.channel, m.getChannel())) {
            return false;
        }
        if (!equalsIgnoreNull(filter.status, m.getStatus())) {
            return false;
        }
        if (!equalsIgnoreNull(filter.slaStatus, m.getSlaStatus())) {
            return false;
        }
        if (filter.fcrStatus != null && !filter.fcrStatus.equals(m.getFcrStatus())) {
            return false;
        }
        if (filter.startDate != null && (m.getCreatedAt() == null || m.getCreatedAt().isBefore(filter.startDate))) {
            return false;
        }
        if (filter.endDate != null && (m.getCreatedAt() == null || m.getCreatedAt().isAfter(filter.endDate))) {
            return false;
        }
        return true;
    }

    private boolean equalsIgnoreNull(String expected, String actual) {
        return expected == null || expected.isBlank() || expected.equalsIgnoreCase(actual);
    }

    private AnalyticsFilter bindAnalyticsFilter(String category, String branch, String district, String channel) {
        AnalyticsFilter filter = new AnalyticsFilter();
        filter.category = category;
        filter.branch = branch;
        filter.district = district;
        filter.channel = channel;
        return filter;
    }

    private void bindAnalyticsRange(AnalyticsFilter filter, String status, String slaStatus, Boolean fcrStatus,
            LocalDateTime startDate, LocalDateTime endDate) {
        filter.status = status;
        filter.slaStatus = slaStatus;
        filter.fcrStatus = fcrStatus;
        filter.startDate = startDate;
        filter.endDate = endDate;
    }

    @GetMapping("/analytics/stats")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getStats(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String branch,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String slaStatus,
            @RequestParam(required = false) Boolean fcrStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        AnalyticsFilter filter = bindAnalyticsFilter(category, branch, district, channel);
        bindAnalyticsRange(filter, status, slaStatus, fcrStatus, startDate, endDate);
        List<ComplaintSlaMetrics> filtered = loadFilteredMetrics(filter);

        long totalCount = filtered.size();
        long closedCount = filtered.stream().filter(SlaStatusRules::isClosedForCompliance).count();
        long fcrCount = filtered.stream().filter(m -> Boolean.TRUE.equals(m.getFcrStatus())).count();
        long slaCompliantCount = filtered.stream()
                .filter(m -> SlaStatusRules.isClosedForCompliance(m) && SlaStatusRules.isResolvedWithinSla(m)).count();
        long overdueCount = filtered.stream().filter(SlaStatusRules::isBreached).count();

        double totalDurationMinutes = 0;
        int durationCount = 0;
        for (var m : filtered) {
            if (STATUS_CLOSED.equalsIgnoreCase(m.getStatus()) && m.getResolvedAt() != null && m.getCreatedAt() != null) {
                long diff = Math.abs(java.time.Duration.between(m.getCreatedAt(), m.getResolvedAt()).toMinutes());
                if (diff == 0)
                    diff = 120; // Default 2 hours if timestamps are identical
                totalDurationMinutes += diff;
                durationCount++;
            }
        }
        double avgResolutionTime = durationCount > 0 ? totalDurationMinutes / durationCount : 0.0;
        double fcrRate = closedCount > 0 ? (fcrCount * 100.0) / closedCount : 0.0;
        double slaComplianceRate = closedCount > 0 ? (slaCompliantCount * 100.0) / closedCount : 100.0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCount", totalCount);
        stats.put("closedCount", closedCount);
        stats.put("fcrCount", fcrCount);
        stats.put("avgResolutionTime", avgResolutionTime);
        stats.put("fcrRate", fcrRate);
        stats.put("slaComplianceRate", slaComplianceRate);
        stats.put("overdueCount", overdueCount);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/analytics/trend")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getTrend(
            @RequestParam(required = false) String interval,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String branch,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String slaStatus,
            @RequestParam(required = false) Boolean fcrStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        AnalyticsFilter filter = bindAnalyticsFilter(category, branch, district, channel);
        bindAnalyticsRange(filter, status, slaStatus, fcrStatus, startDate, endDate);
        List<ComplaintSlaMetrics> filtered = loadFilteredMetrics(filter);

        String formatStr = "yyyy-MM-dd";
        if ("weekly".equalsIgnoreCase(interval)) {
            formatStr = "yyyy-'W'ww";
        } else if ("monthly".equalsIgnoreCase(interval)) {
            formatStr = "yyyy-MM";
        } else if ("yearly".equalsIgnoreCase(interval)) {
            formatStr = "yyyy";
        }

        final String activeFormat = formatStr;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(activeFormat);

        Map<String, Long> grouped = filtered.stream()
                .filter(m -> m.getCreatedAt() != null)
                .collect(Collectors.groupingBy(m -> m.getCreatedAt().format(formatter), Collectors.counting()));

        List<Map<String, Object>> trend = grouped.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("period", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                })
                .sorted(Comparator.comparing(m -> m.get("period").toString()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(trend);
    }

    @GetMapping("/analytics/reports")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getReports(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String branch,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String slaStatus,
            @RequestParam(required = false) Boolean fcrStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        AnalyticsFilter filter = bindAnalyticsFilter(category, branch, district, channel);
        bindAnalyticsRange(filter, status, slaStatus, fcrStatus, startDate, endDate);
        List<ComplaintSlaMetrics> filtered = loadFilteredMetrics(filter);

        List<Map<String, Object>> enrichedReports = new ArrayList<>();
        for (ComplaintSlaMetrics m : filtered) {
            enrichedReports.add(buildEnrichedReport(m));
        }
        return ResponseEntity.ok(enrichedReports);
    }

    private Map<String, Object> buildEnrichedReport(ComplaintSlaMetrics m) {
        Map<String, Object> map = baseReportMap(m);
        enrichFromHistoricVariables(map, m);
        enrichFromComplaintsTable(map, m);
        return map;
    }

    private Map<String, Object> baseReportMap(ComplaintSlaMetrics m) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", m.getId());
        map.put("processInstanceId", m.getProcessInstanceId());
        map.put(COMPLAINT_ID, m.getComplaintId());
        map.put(GENERAL_TICKET_ID, m.getGeneralTicketId());
        map.put("dbcTicketId",
                m.getComplaintId() != null && m.getComplaintId().startsWith("DBC-") ? m.getComplaintId()
                        : m.getGeneralTicketId());
        map.put("complaintCategory", m.getComplaintCategory());
        map.put("category", m.getComplaintCategory());
        map.put("classification", m.getClassification());
        map.put("priority", m.getPriority());
        map.put("complaintClassification", m.getPriority());
        map.put("branch", m.getBranch());
        map.put("district", m.getDistrict());
        map.put("department", m.getDepartment());
        map.put("channel", m.getChannel());
        map.put("fcrStatus", m.getFcrStatus());
        map.put("customerName", m.getCustomerName());
        map.put("status", m.getStatus());
        map.put("overallStatus", slaTrackingService.resolveOverallStatus(m));
        map.put("currentStage", m.getCurrentStage());
        map.put("slaStatus", m.getSlaStatus());
        map.put("breached", m.getBreached());
        map.put(CREATED_AT, m.getCreatedAt());
        map.put("resolvedAt", m.getResolvedAt());
        map.put("deadline", m.getDeadline());
        return map;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asStringObjectMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : Map.of();
    }

    private void putIfHasText(Map<String, Object> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value);
        }
    }

    private void putIfBlank(Map<String, Object> map, String key, Object dbValue) {
        Object existing = map.get(key);
        if (existing == null || existing.toString().isBlank()) {
            map.put(key, dbValue);
        }
    }

    private void enrichFromHistoricVariables(Map<String, Object> map, ComplaintSlaMetrics m) {
        if (m.getProcessInstanceId() == null) {
            return;
        }
        try {
            var hpi = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(m.getProcessInstanceId())
                    .includeProcessVariables()
                    .singleResult();
            if (hpi == null || hpi.getProcessVariables() == null) {
                return;
            }
            Map<String, Object> vars = hpi.getProcessVariables();
            map.put("variables", vars);

            Map<String, Object> cust = asStringObjectMap(vars.get("customer"));
            Map<String, Object> comp = asStringObjectMap(vars.get("complaint"));

            String acc = (String) vars.getOrDefault(ACCOUNT_NUMBER,
                    cust.getOrDefault(ACCOUNT_NUMBER, comp.getOrDefault(ACCOUNT_NUMBER, "")));
            String phone = (String) vars.getOrDefault(PHONE, cust.getOrDefault(PHONE,
                    cust.getOrDefault("contactPhone", comp.getOrDefault("contactPhone", ""))));
            String desc = (String) vars.getOrDefault(DESCRIPTION,
                    vars.getOrDefault("complaintDescription", comp.getOrDefault(DESCRIPTION, "")));
            String evUrl = (String) vars.getOrDefault(EVIDENCE_URL, comp.getOrDefault(EVIDENCE_URL, ""));
            String voiceUrl = (String) vars.getOrDefault(VOICE_ATTACHMENT_URL,
                    comp.getOrDefault(VOICE_ATTACHMENT_URL, ""));

            putIfHasText(map, ACCOUNT_NUMBER, acc);
            putIfHasText(map, PHONE, phone);
            putIfHasText(map, DESCRIPTION, desc);
            putIfHasText(map, EVIDENCE_URL, evUrl);
            putIfHasText(map, VOICE_ATTACHMENT_URL, voiceUrl);
        } catch (Exception e) {
            log.debug("Skipping historic variable enrichment for process {}: {}", m.getProcessInstanceId(), e.getMessage());
        }
    }

    private void enrichFromComplaintsTable(Map<String, Object> map, ComplaintSlaMetrics m) {
        try {
            List<Map<String, Object>> compDb = jdbcTemplate.queryForList(
                    "SELECT account_number, preferred_contact_number, complaint_detail, evidence_url, voice_attachment_url FROM complaints WHERE process_instance_id = ? OR ticket_number = ? OR general_ticket_id = ?",
                    m.getProcessInstanceId(), m.getComplaintId(), m.getGeneralTicketId());
            if (compDb.isEmpty()) {
                return;
            }
            Map<String, Object> row = compDb.get(0);
            putIfBlank(map, ACCOUNT_NUMBER, row.get("account_number"));
            putIfBlank(map, PHONE, row.get("preferred_contact_number"));
            putIfBlank(map, DESCRIPTION, row.get("complaint_detail"));
            putIfBlank(map, EVIDENCE_URL, row.get("evidence_url"));
            putIfBlank(map, VOICE_ATTACHMENT_URL, row.get("voice_attachment_url"));
        } catch (Exception e) {
            log.debug("Skipping complaints-table enrichment for ticket {}: {}", m.getComplaintId(), e.getMessage());
        }
    }

    @GetMapping("/analytics/export")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> exportReports(
            @RequestParam(required = false) String format,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String branch,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String slaStatus,
            @RequestParam(required = false) Boolean fcrStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        AnalyticsFilter filter = bindAnalyticsFilter(category, branch, district, channel);
        bindAnalyticsRange(filter, status, slaStatus, fcrStatus, startDate, endDate);
        List<ComplaintSlaMetrics> filtered = loadFilteredMetrics(filter);

        StringBuilder sb = new StringBuilder();
        sb.append(
                "Ticket ID,Customer Name,Category,Branch,District,Channel,Status,SLA Status,Resolution Time (min),FCR Status,Created At,Resolved At\n");

        for (var m : filtered) {
            long duration = 0;
            if (m.getResolvedAt() != null) {
                duration = java.time.Duration.between(m.getCreatedAt(), m.getResolvedAt()).toMinutes();
            }
            sb.append(String.format("%s,\"%s\",%s,%s,%s,%s,%s,%s,%d,%s,%s,%s\n",
                    m.getComplaintId(),
                    m.getCustomerName() != null ? m.getCustomerName().replace("\"", "\"\"") : "",
                    m.getComplaintCategory(),
                    m.getBranch(),
                    m.getDistrict(),
                    m.getChannel(),
                    m.getStatus(),
                    m.getSlaStatus(),
                    duration,
                    Boolean.TRUE.equals(m.getFcrStatus()) ? "FCR" : "Standard Escalation",
                    m.getCreatedAt(),
                    m.getResolvedAt() != null ? m.getResolvedAt().toString() : ""));
        }

        byte[] data = sb.toString().getBytes(StandardCharsets.UTF_8);
        String filename = "csv".equalsIgnoreCase(format) ? "complaint_report.csv" : "complaint_report.xls";
        String contentType = "csv".equalsIgnoreCase(format) ? "text/csv" : "application/vnd.ms-excel";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(data.length)
                .body(data);
    }
}
