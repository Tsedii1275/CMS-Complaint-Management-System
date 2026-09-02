package com.dashenbank.cms.controller;

import com.dashenbank.cms.model.CapaAction;
import com.dashenbank.cms.model.Rca5Whys;
import com.dashenbank.cms.model.RcaAuditLog;
import com.dashenbank.cms.model.RcaCase;
import com.dashenbank.cms.repository.CapaActionRepository;
import com.dashenbank.cms.repository.Rca5WhysRepository;
import com.dashenbank.cms.repository.RcaAuditLogRepository;
import com.dashenbank.cms.repository.RcaCaseRepository;
import com.dashenbank.cms.service.CapaNatureAnalysisService;
import com.dashenbank.cms.service.RcaAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rca")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class RcaController {

    private static final String KEY_RCA_REQUIRED = "rcaRequired";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_RCA_CASE = "rcaCase";
    private static final String KEY_OWNER = "owner";
    private static final String KEY_TARGET_DATE = "targetDate";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String CONTENT_DISPOSITION_ATTACHMENT = "attachment";

    private final RcaCaseRepository rcaCaseRepository;
    private final Rca5WhysRepository rca5WhysRepository;
    private final CapaActionRepository capaActionRepository;
    private final RcaAuditLogRepository rcaAuditLogRepository;
    private final RcaAnalysisService rcaAnalysisService;
    private final CapaNatureAnalysisService capaNatureAnalysisService;

    @Autowired
    public RcaController(
            RcaCaseRepository rcaCaseRepository,
            Rca5WhysRepository rca5WhysRepository,
            CapaActionRepository capaActionRepository,
            RcaAuditLogRepository rcaAuditLogRepository,
            RcaAnalysisService rcaAnalysisService,
            CapaNatureAnalysisService capaNatureAnalysisService) {
        this.rcaCaseRepository = rcaCaseRepository;
        this.rca5WhysRepository = rca5WhysRepository;
        this.capaActionRepository = capaActionRepository;
        this.rcaAuditLogRepository = rcaAuditLogRepository;
        this.rcaAnalysisService = rcaAnalysisService;
        this.capaNatureAnalysisService = capaNatureAnalysisService;
    }

    @GetMapping("/root-cause-analysis")
    public ResponseEntity<Map<String, Object>> getRootCauseAnalysis(@ModelAttribute RcaQuery query) {
        return ResponseEntity.ok(rcaAnalysisService.analyze(bindFilter(query)));
    }

    @GetMapping("/root-cause-analysis/export")
    public ResponseEntity<byte[]> exportRootCauseAnalysis(
            @RequestParam(defaultValue = "csv") String format,
            @ModelAttribute RcaQuery query) {
        String csv = rcaAnalysisService.exportCsv(bindFilter(query));
        boolean excel = "excel".equalsIgnoreCase(format) || "xls".equalsIgnoreCase(format);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                excel ? "application/vnd.ms-excel" : "text/csv"));
        headers.setContentDispositionFormData(CONTENT_DISPOSITION_ATTACHMENT,
                excel ? "Root_Cause_Analysis.xls" : "Root_Cause_Analysis.csv");
        return new ResponseEntity<>(csv.getBytes(StandardCharsets.UTF_8), headers, HttpStatus.OK);
    }

    @GetMapping("/nature-capa")
    public ResponseEntity<Map<String, Object>> getNatureCapa(@RequestParam String nature) {
        return ResponseEntity.ok(capaNatureAnalysisService.getByNature(nature));
    }

    @PutMapping("/nature-capa")
    public ResponseEntity<Map<String, Object>> saveNatureCapa(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(capaNatureAnalysisService.save(payload, getCurrentUsername()));
    }

    public static class RcaQuery {
        public String nature;
        public String category;
        public String status;
        public String channel;
        public String branch;
        public String district;
        public String department;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        public LocalDate fromDate;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        public LocalDate toDate;

        public void setNature(String nature) {
            this.nature = nature;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public void setChannel(String channel) {
            this.channel = channel;
        }

        public void setBranch(String branch) {
            this.branch = branch;
        }

        public void setDistrict(String district) {
            this.district = district;
        }

        public void setDepartment(String department) {
            this.department = department;
        }

        public void setFromDate(LocalDate fromDate) {
            this.fromDate = fromDate;
        }

        public void setToDate(LocalDate toDate) {
            this.toDate = toDate;
        }
    }

    private RcaAnalysisService.RcaFilter bindFilter(RcaQuery query) {
        RcaAnalysisService.RcaFilter filter = new RcaAnalysisService.RcaFilter();
        if (query == null) {
            return filter;
        }
        filter.nature = query.nature;
        filter.category = query.category;
        filter.status = query.status;
        filter.channel = query.channel;
        filter.branch = query.branch;
        filter.district = query.district;
        filter.department = query.department;
        filter.fromDate = query.fromDate;
        filter.toDate = query.toDate;
        return filter;
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return auth != null ? auth.getName() : "system";
    }

    private void logAudit(Long rcaCaseId, String ticketId, String action, String description) {
        RcaAuditLog auditLog = RcaAuditLog.builder()
                .rcaCaseId(rcaCaseId)
                .ticketId(ticketId)
                .action(action)
                .actor(getCurrentUsername())
                .description(description)
                .build();
        rcaAuditLogRepository.save(auditLog);
    }

    @PostMapping("/trigger")
    @Transactional
    public ResponseEntity<Object> triggerRca(@RequestBody Map<String, Object> payload) {
        String ticketId = (String) payload.get("ticketId");
        String processInstanceId = (String) payload.get("processInstanceId");
        Boolean rcaRequired = (Boolean) payload.get(KEY_RCA_REQUIRED);

        if (ticketId == null || ticketId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "ticketId is required"));
        }

        Optional<RcaCase> existing = rcaCaseRepository.findByTicketId(ticketId);
        if (existing.isPresent()) {
            return ResponseEntity.ok(Map.of(KEY_MESSAGE, "RCA Case already exists", KEY_RCA_CASE, existing.get()));
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());

        if (Boolean.TRUE.equals(rcaRequired)) {
            RcaCase newCase = RcaCase.builder()
                    .ticketId(ticketId)
                    .processInstanceId(processInstanceId)
                    .rcaStatus(STATUS_PENDING)
                    .rcaRequired(true)
                    .incidentDate(now)
                    .build();

            RcaCase savedCase = rcaCaseRepository.save(newCase);

            Rca5Whys whys = Rca5Whys.builder()
                    .rcaCase(savedCase)
                    .build();
            rca5WhysRepository.save(whys);
            savedCase.setWhys(whys);

            logAudit(savedCase.getId(), ticketId, "RCA_TRIGGERED", "Root Cause Analysis triggered by " + getCurrentUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(savedCase);
        } else {
            RcaCase newCase = RcaCase.builder()
                    .ticketId(ticketId)
                    .processInstanceId(processInstanceId)
                    .rcaStatus("NOT_REQUIRED")
                    .rcaRequired(false)
                    .incidentDate(now)
                    .build();
            RcaCase savedCase = rcaCaseRepository.save(newCase);
            logAudit(savedCase.getId(), ticketId, "RCA_DECLINED", "RCA marked as not required by " + getCurrentUsername());
            return ResponseEntity.ok(Map.of(KEY_MESSAGE, "RCA not requested for ticket " + ticketId, KEY_RCA_CASE, savedCase));
        }
    }

    @GetMapping("/cases")
    public ResponseEntity<List<RcaCase>> getAllCases() {
        return ResponseEntity.ok(rcaCaseRepository.findAll());
    }

    @GetMapping("/cases/{id}")
    public ResponseEntity<RcaCase> getCaseById(@PathVariable Long id) {
        return rcaCaseRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/cases/{id}")
    @Transactional
    public ResponseEntity<Object> updateCase(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Optional<RcaCase> opt = rcaCaseRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        RcaCase rcaCase = opt.get();
        String oldStatus = rcaCase.getRcaStatus();

        applyPayloadToCase(rcaCase, payload);
        calculateRiskScore(rcaCase);

        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        if (STATUS_PENDING.equals(oldStatus) && !STATUS_PENDING.equals(rcaCase.getRcaStatus())) {
            rcaCase.setAnalysisDate(now);
        }
        if (!STATUS_COMPLETED.equals(oldStatus) && STATUS_COMPLETED.equals(rcaCase.getRcaStatus())) {
            rcaCase.setRcaCompletionDate(now);
        }

        RcaCase saved = rcaCaseRepository.save(rcaCase);
        logAudit(id, saved.getTicketId(), "RCA_UPDATED", "RCA details updated. Status: " + saved.getRcaStatus());

        return ResponseEntity.ok(saved);
    }

    private void applyPayloadToCase(RcaCase rcaCase, Map<String, Object> payload) {
        if (payload.containsKey("rootCauseCategory")) {
            rcaCase.setRootCauseCategory((String) payload.get("rootCauseCategory"));
        }
        if (payload.containsKey("financialImpact")) {
            Object fi = payload.get("financialImpact");
            if (fi != null) {
                rcaCase.setFinancialImpact(new BigDecimal(fi.toString()));
            }
        }
        if (payload.containsKey("reputationalRisk")) {
            rcaCase.setReputationalRisk((String) payload.get("reputationalRisk"));
        }
        if (payload.containsKey("complianceImpact")) {
            rcaCase.setComplianceImpact((String) payload.get("complianceImpact"));
        }
        if (payload.containsKey("operationalDisruption")) {
            rcaCase.setOperationalDisruption((String) payload.get("operationalDisruption"));
        }
        if (payload.containsKey("preventiveStrategy")) {
            rcaCase.setPreventiveStrategy((String) payload.get("preventiveStrategy"));
        }
        if (payload.containsKey("rcaStatus")) {
            rcaCase.setRcaStatus((String) payload.get("rcaStatus"));
        }
        if (payload.containsKey(KEY_RCA_REQUIRED)) {
            rcaCase.setRcaRequired((Boolean) payload.get(KEY_RCA_REQUIRED));
        }
        if (payload.containsKey("rcaSummary")) {
            rcaCase.setRcaSummary((String) payload.get("rcaSummary"));
        }
        if (payload.containsKey("rcaOwner")) {
            rcaCase.setRcaOwner((String) payload.get("rcaOwner"));
        }
        if (payload.containsKey("rcaCompletionDate")) {
            Object rcd = payload.get("rcaCompletionDate");
            if (rcd != null) {
                rcaCase.setRcaCompletionDate(LocalDateTime.parse(rcd.toString()));
            }
        }
    }

    private void calculateRiskScore(RcaCase rcaCase) {
        double riskVal = 0.0;
        if (rcaCase.getFinancialImpact() != null) {
            riskVal += rcaCase.getFinancialImpact().doubleValue() / 1000.0;
        }
        if ("MEDIUM".equalsIgnoreCase(rcaCase.getReputationalRisk())) {
            riskVal += 5;
        } else if ("HIGH".equalsIgnoreCase(rcaCase.getReputationalRisk())) {
            riskVal += 10;
        } else if ("CRITICAL".equalsIgnoreCase(rcaCase.getReputationalRisk())) {
            riskVal += 20;
        }
        rcaCase.setRiskScore(riskVal);
    }

    @PutMapping("/cases/{id}/whys")
    @Transactional
    public ResponseEntity<Object> updateWhys(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        Optional<RcaCase> opt = rcaCaseRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        RcaCase rcaCase = opt.get();
        Rca5Whys whys = rcaCase.getWhys();
        if (whys == null) {
            whys = new Rca5Whys();
            whys.setRcaCase(rcaCase);
        }

        whys.setWhy1(payload.get("why1"));
        whys.setWhy2(payload.get("why2"));
        whys.setWhy3(payload.get("why3"));
        whys.setWhy4(payload.get("why4"));
        whys.setWhy5(payload.get("why5"));
        whys.setRootCauseStatement(payload.get("rootCauseStatement"));

        Rca5Whys savedWhys = rca5WhysRepository.save(whys);
        rcaCase.setWhys(savedWhys);
        rcaCaseRepository.save(rcaCase);

        logAudit(id, rcaCase.getTicketId(), "WHYS_UPDATED", "5 Whys model updated.");
        return ResponseEntity.ok(savedWhys);
    }

    @PostMapping("/cases/{id}/capa")
    @Transactional
    public ResponseEntity<Object> addCapaAction(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Optional<RcaCase> opt = rcaCaseRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        RcaCase rcaCase = opt.get();
        CapaAction action = CapaAction.builder()
                .rcaCase(rcaCase)
                .actionType((String) payload.get("actionType"))
                .actionDescription((String) payload.get("actionDescription"))
                .owner((String) payload.get(KEY_OWNER))
                .targetDate(payload.get(KEY_TARGET_DATE) != null ? LocalDate.parse((String) payload.get(KEY_TARGET_DATE)) : null)
                .implementationStatus(STATUS_PENDING)
                .build();

        CapaAction saved = capaActionRepository.save(action);
        logAudit(id, rcaCase.getTicketId(), "CAPA_ADDED", "New CAPA action added. Action ID: " + saved.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/capa/{actionId}")
    @Transactional
    public ResponseEntity<Object> updateCapaAction(@PathVariable Long actionId, @RequestBody Map<String, Object> payload) {
        Optional<CapaAction> opt = capaActionRepository.findById(actionId);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        CapaAction action = opt.get();
        if (payload.containsKey("implementationStatus")) {
            action.setImplementationStatus((String) payload.get("implementationStatus"));
        }
        if (payload.containsKey(KEY_OWNER)) {
            action.setOwner((String) payload.get(KEY_OWNER));
        }
        if (payload.containsKey(KEY_TARGET_DATE)) {
            action.setTargetDate(payload.get(KEY_TARGET_DATE) != null ? LocalDate.parse((String) payload.get(KEY_TARGET_DATE)) : null);
        }
        if (payload.containsKey("effectivenessRating")) {
            action.setEffectivenessRating((String) payload.get("effectivenessRating"));
        }
        if (payload.containsKey("verificationNotes")) {
            action.setVerificationNotes((String) payload.get("verificationNotes"));
        }

        CapaAction saved = capaActionRepository.save(action);
        logAudit(action.getRcaCase().getId(), action.getRcaCase().getTicketId(), "CAPA_UPDATED", "CAPA action updated. Status: " + saved.getImplementationStatus());

        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/capa/{actionId}")
    @Transactional
    public ResponseEntity<Object> deleteCapaAction(@PathVariable Long actionId) {
        Optional<CapaAction> opt = capaActionRepository.findById(actionId);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        CapaAction action = opt.get();
        Long rcaCaseId = action.getRcaCase().getId();
        String ticketId = action.getRcaCase().getTicketId();

        capaActionRepository.delete(action);
        logAudit(rcaCaseId, ticketId, "CAPA_DELETED", "CAPA action deleted. Action ID: " + actionId);

        return ResponseEntity.ok(Map.of(KEY_MESSAGE, "CAPA action deleted"));
    }

    @GetMapping("/cases/{id}/audit-logs")
    public ResponseEntity<List<RcaAuditLog>> getAuditLogs(@PathVariable Long id) {
        return ResponseEntity.ok(rcaAuditLogRepository.findByRcaCaseId(id));
    }

    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> getAnalytics() {
        List<RcaCase> cases = rcaCaseRepository.findAll();
        List<CapaAction> capaActions = capaActionRepository.findAll();

        long total = cases.size();
        long completed = cases.stream().filter(c -> STATUS_COMPLETED.equalsIgnoreCase(c.getRcaStatus())).count();
        long inProgress = cases.stream().filter(c -> "IN_PROGRESS".equalsIgnoreCase(c.getRcaStatus())).count();
        long pending = cases.stream().filter(c -> STATUS_PENDING.equalsIgnoreCase(c.getRcaStatus())).count();

        BigDecimal totalFinancialImpact = cases.stream()
                .map(RcaCase::getFinancialImpact)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Long> categoryBreakdown = cases.stream()
                .map(RcaCase::getRootCauseCategory)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(c -> c, Collectors.counting()));

        Map<String, Long> riskBreakdown = cases.stream()
                .map(RcaCase::getReputationalRisk)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(c -> c, Collectors.counting()));

        Map<String, Long> capaStatusBreakdown = capaActions.stream()
                .map(CapaAction::getImplementationStatus)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(c -> c, Collectors.counting()));

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("completed", completed);
        stats.put("inProgress", inProgress);
        stats.put("pending", pending);
        stats.put("totalFinancialImpact", totalFinancialImpact);
        stats.put("categoryBreakdown", categoryBreakdown);
        stats.put("riskBreakdown", riskBreakdown);
        stats.put("capaStatusBreakdown", capaStatusBreakdown);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/cases/export/excel")
    public ResponseEntity<byte[]> exportExcel() {
        List<RcaCase> cases = rcaCaseRepository.findAll();
        StringBuilder csv = new StringBuilder();
        csv.append("id,Ticket ID,Process Instance ID,Root Cause Category,Incident Date,RCA Status,Financial Impact,Reputational Risk,Risk Score,Created At\n");
        for (RcaCase c : cases) {
            csv.append(c.getId()).append(",")
                    .append(c.getTicketId()).append(",")
                    .append(c.getProcessInstanceId() != null ? c.getProcessInstanceId() : "").append(",")
                    .append(c.getRootCauseCategory() != null ? c.getRootCauseCategory() : "").append(",")
                    .append(c.getIncidentDate() != null ? c.getIncidentDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "").append(",")
                    .append(c.getRcaStatus()).append(",")
                    .append(c.getFinancialImpact() != null ? c.getFinancialImpact() : BigDecimal.ZERO).append(",")
                    .append(c.getReputationalRisk() != null ? c.getReputationalRisk() : "").append(",")
                    .append(c.getRiskScore() != null ? c.getRiskScore() : 0.0).append(",")
                    .append(c.getCreatedAt() != null ? c.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "").append("\n");
        }

        byte[] outputBytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData(CONTENT_DISPOSITION_ATTACHMENT, "rca_cases_export.csv");

        return new ResponseEntity<>(outputBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/cases/export/pdf")
    public ResponseEntity<byte[]> exportPdf() {
        List<RcaCase> cases = rcaCaseRepository.findAll();
        StringBuilder pdf = new StringBuilder();
        String nl = System.lineSeparator();
        pdf.append("============================================================").append(nl);
        pdf.append("            ROOT CAUSE ANALYSIS (RCA) CASES REPORT          ").append(nl);
        pdf.append("============================================================").append(nl).append(nl);
        pdf.append("Generated: ").append(LocalDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append(nl).append(nl);

        for (RcaCase c : cases) {
            pdf.append("------------------------------------------------------------").append(nl);
            pdf.append("Ticket ID:       ").append(c.getTicketId()).append(nl);
            pdf.append("Status:          ").append(c.getRcaStatus()).append(nl);
            pdf.append("Category:        ").append(c.getRootCauseCategory() != null ? c.getRootCauseCategory() : "N/A").append(nl);
            pdf.append("Financial Loss:  ETB ").append(c.getFinancialImpact() != null ? c.getFinancialImpact() : "0.00").append(nl);
            pdf.append("Reputational:    ").append(c.getReputationalRisk()).append(nl);
            pdf.append("Risk Score:      ").append(c.getRiskScore()).append(nl);
            pdf.append("Strategy:        ").append(c.getPreventiveStrategy() != null ? c.getPreventiveStrategy() : "N/A").append(nl);
            pdf.append("------------------------------------------------------------").append(nl).append(nl);
        }

        byte[] outputBytes = pdf.toString().getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setContentDispositionFormData(CONTENT_DISPOSITION_ATTACHMENT, "rca_cases_report.txt");

        return new ResponseEntity<>(outputBytes, headers, HttpStatus.OK);
    }
}

