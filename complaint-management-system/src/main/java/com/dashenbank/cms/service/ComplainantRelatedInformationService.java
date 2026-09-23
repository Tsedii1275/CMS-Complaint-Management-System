package com.dashenbank.cms.service;

import com.dashenbank.cms.customer.CustomerContactPhones;
import com.dashenbank.cms.customer.LocationKeys;
import com.dashenbank.cms.model.AuditLog;
import com.dashenbank.cms.model.ComplainantRelatedInformation;
import com.dashenbank.cms.model.ComplaintSlaMetrics;
import com.dashenbank.cms.model.OverallComplaintStatus;
import com.dashenbank.cms.repository.AuditLogRepository;
import com.dashenbank.cms.repository.ComplainantRelatedInformationRepository;
import com.dashenbank.cms.repository.ComplaintSlaMetricsRepository;
import com.dashenbank.cms.util.TicketNumberSort;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Comparator;
import java.util.function.Consumer;

@Service
public class ComplainantRelatedInformationService {

    private static final Logger log = LoggerFactory.getLogger(ComplainantRelatedInformationService.class);
    private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();
    private static final String KEY_BRANCH = "branch";
    private static final String KEY_COMPLAINT = "complaint";
    private static final String KEY_CUSTOMER = "customer";
    private static final String KEY_CLASSIFICATION = "classification";
    private static final String KEY_ACCOUNT_NUMBER = "accountNumber";
    private static final String KEY_ACCOUNT_NUMBER_SNAKE = "account_number";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_PREFERRED_CONTACT_NUMBER = "preferredContactNumber";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_SERVICE_TYPE = "serviceType";
    private static final String KEY_SERVICE_TYPE_SNAKE = "service_type";
    private static final String KEY_EVIDENCE_NAME = "evidenceName";
    private static final String KEY_RECEIVED_BY = "receivedBy";
    private static final String KEY_RECEIVED_BY_SNAKE = "received_by";
    private static final String KEY_COMPLAINT_CLASSIFICATION = "complaintClassification";
    private static final String KEY_PRIORITY_LEVEL = "priorityLevel";
    private static final String KEY_COMPLAINT_CLASSIFICATION_SNAKE = "complaint_classification";
    private static final String KEY_DETAILS_OF_COMPLAINT = "detailsOfComplaint";
    private static final String KEY_UNIQUE_ID_NO = "uniqueIdNo";

    private final ComplainantRelatedInformationRepository repository;
    private final ComplaintSlaMetricsRepository slaMetricsRepository;
    private final AuditLogRepository auditLogRepository;
    private final BusinessHoursService businessHoursService;
    private final RuntimeService runtimeService;
    private final HistoryService historyService;

    @Autowired(required = false)
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    public ComplainantRelatedInformationService(
            ComplainantRelatedInformationRepository repository,
            ComplaintSlaMetricsRepository slaMetricsRepository,
            AuditLogRepository auditLogRepository,
            BusinessHoursService businessHoursService,
            ObjectProvider<RuntimeService> runtimeServiceProvider,
            ObjectProvider<HistoryService> historyServiceProvider) {
        this.repository = repository;
        this.slaMetricsRepository = slaMetricsRepository;
        this.auditLogRepository = auditLogRepository;
        this.businessHoursService = businessHoursService;
        this.runtimeService = runtimeServiceProvider.getIfAvailable();
        this.historyService = historyServiceProvider.getIfAvailable();
    }

    @PostConstruct
    public void initSyncOnStartup() {
        try {
            syncAllExistingComplaints();
        } catch (Exception e) {
            log.error("Startup sync for ComplainantRelatedInformation failed: {}", e.getMessage());
        }
    }

    private String defaultIfBlank(String val) {
        if (val == null || val.trim().isEmpty() || "null".equalsIgnoreCase(val.trim())) {
            return "-";
        }
        return val.trim();
    }

    private String defaultIfBlank(String val, String fallback) {
        if (val == null || val.trim().isEmpty() || "null".equalsIgnoreCase(val.trim())) {
            return defaultIfBlank(fallback);
        }
        return val.trim();
    }

    /**
     * Standardized Status Normalization
     */
    public String normalizeStatus(String rawStatus) {
        return OverallComplaintStatus.canonicalize(rawStatus);
    }

    private String resolveCaseStatusFromMetrics(ComplaintSlaMetrics metrics) {
        if (metrics == null) {
            return OverallComplaintStatus.RECORDED;
        }
        if (OverallComplaintStatus.isOfficial(metrics.getStatus())) {
            return OverallComplaintStatus.canonicalize(metrics.getStatus());
        }
        if (OverallComplaintStatus.isOfficial(metrics.getOverallStatus())) {
            return OverallComplaintStatus.canonicalize(metrics.getOverallStatus());
        }
        Map<String, Object> vars = new HashMap<>();
        vars.put("status", metrics.getStatus());
        vars.put("currentStage", metrics.getCurrentStage());
        vars.put(KEY_CLASSIFICATION, metrics.getClassification());
        vars.put("requiresInvestigation", metrics.getRequiresInvestigation());
        vars.put("department", metrics.getDepartment());
        return OverallComplaintStatus.resolve(vars, false);
    }

    private void overlayLiveCaseStatus(ComplainantRelatedInformation info) {
        if (info == null || info.getUniqueIdNo() == null || info.getUniqueIdNo().isBlank()) {
            return;
        }
        String ticketId = info.getUniqueIdNo();
        Optional<ComplaintSlaMetrics> metrics = slaMetricsRepository.findByComplaintId(ticketId);
        if (metrics.isEmpty()) {
            metrics = slaMetricsRepository.findByGeneralTicketId(ticketId);
        }
        if (metrics.isEmpty()) {
            metrics = slaMetricsRepository.findByDbcTicketId(ticketId);
        }
        metrics.ifPresent(m -> {
            if (!Boolean.TRUE.equals(info.getIsManuallyEdited())) {
                info.setCaseStatus(resolveCaseStatusFromMetrics(m));
            }
        });
    }

    /**
     * Calculate resolution time in working days between complaint date and
     * resolution date.
     */
    public Integer calculateWorkingDays(LocalDateTime start, LocalDateTime end) {
        if (start == null)
            return 0;
        LocalDateTime toDate = end != null ? end : LocalDateTime.now(SYSTEM_ZONE);
        if (toDate.isBefore(start))
            return 0;

        int workingDays = 0;
        LocalDate curr = start.toLocalDate();
        LocalDate last = toDate.toLocalDate();

        while (!curr.isAfter(last)) {
            if (businessHoursService.isWorkingDay(curr)) {
                workingDays++;
            }
            curr = curr.plusDays(1);
        }
        return workingDays;
    }

    private String resolveUniqueTicketId(String formalId, String generalId) {
        if (formalId != null && formalId.startsWith("DBC-")) {
            return formalId;
        }
        if (formalId != null) {
            return formalId;
        }
        return generalId;
    }

    private Optional<ComplainantRelatedInformation> findExistingRecord(String ticketId, String generalId) {
        Optional<ComplainantRelatedInformation> opt = repository.findByUniqueIdNo(ticketId);
        if (opt.isEmpty() && generalId != null && !generalId.trim().isEmpty() && !generalId.equals(ticketId)) {
            opt = repository.findByUniqueIdNo(generalId);
            if (opt.isPresent()) {
                ComplainantRelatedInformation existingGen = opt.get();
                existingGen.setUniqueIdNo(ticketId);
                repository.save(existingGen);
            }
        }
        return opt;
    }

    private void deleteLegacyDuplicateIfPresent(String ticketId, String generalId,
            Optional<ComplainantRelatedInformation> opt) {
        if (generalId == null || generalId.equals(ticketId) || opt.isEmpty()) {
            return;
        }
        Optional<ComplainantRelatedInformation> legacyOpt = repository.findByUniqueIdNo(generalId);
        if (legacyOpt.isPresent() && !legacyOpt.get().getId().equals(opt.get().getId())) {
            try {
                repository.delete(legacyOpt.get());
            } catch (Exception e) {
                log.debug("Could not delete legacy CM complainant record {}: {}", generalId, e.getMessage());
            }
        }
    }

    @Transactional
    public ComplainantRelatedInformation syncFromSlaMetrics(ComplaintSlaMetrics metrics) {
        if (metrics == null) {
            return null;
        }
        if (!SlaTrackingService.isClassifiedComplaint(metrics)) {
            return null;
        }

        String formalId = metrics.getComplaintId();
        String generalId = metrics.getGeneralTicketId();
        String ticketId = resolveUniqueTicketId(formalId, generalId);
        if (ticketId == null || ticketId.trim().isEmpty()) {
            return null;
        }

        Optional<ComplainantRelatedInformation> opt = findExistingRecord(ticketId, generalId);
        deleteLegacyDuplicateIfPresent(ticketId, generalId, opt);

        ComplainantRelatedInformation info = opt.orElseGet(() -> {
            ComplainantRelatedInformation newRecord = new ComplainantRelatedInformation();
            newRecord.setUniqueIdNo(ticketId);
            newRecord.setIsManuallyEdited(false);
            return newRecord;
        });

        info.setUniqueIdNo(ticketId);
        boolean isManual = Boolean.TRUE.equals(info.getIsManuallyEdited());

        if (!isManual) {
            info.setNameOfComplainant(defaultIfBlank(metrics.getCustomerName(), info.getNameOfComplainant()));
            info.setComplaintsCategory(defaultIfBlank(metrics.getComplaintCategory(), info.getComplaintsCategory()));
            info.setDistrictDepartment(defaultIfBlank(metrics.getDistrict(), info.getDistrictDepartment()));
            info.setComplaintMadeOnChannel(defaultIfBlank(metrics.getChannel(), info.getComplaintMadeOnChannel()));
            hydrateComplaintIdentityFields(info, metrics);
        }

        if (info.getDateOfComplaint() == null && metrics.getCreatedAt() != null) {
            info.setDateOfComplaint(metrics.getCreatedAt());
        }
        if (metrics.getDeadline() != null && (!isManual || info.getExpectedResolutionDate() == null)) {
            info.setExpectedResolutionDate(metrics.getDeadline());
        }
        if (metrics.getResolvedAt() != null && (!isManual || info.getActualResolutionDate() == null)) {
            info.setActualResolutionDate(metrics.getResolvedAt());
        }

        if (!isManual) {
            info.setCaseStatus(resolveCaseStatusFromMetrics(metrics));
        }
        info.setResolutionTimeWorkingDays(
                calculateWorkingDays(info.getDateOfComplaint(), info.getActualResolutionDate()));
        applyDefaultsIfMissing(info);

        return repository.save(info);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asStringObjectMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : null;
    }

    private String nestedOrVar(Map<String, Object> nested, Map<String, Object> vars, String nestedKey, String varKey) {
        String nestedValue = text(nested != null ? nested.get(nestedKey) : null);
        if (nestedValue != null) {
            return nestedValue;
        }
        return text(vars != null ? vars.get(varKey) : null);
    }

    private String firstVar(Map<String, Object> vars, String primary, String fallback) {
        String value = text(vars != null ? vars.get(primary) : null);
        if (value == null) {
            value = text(vars != null ? vars.get(fallback) : null);
        }
        return value;
    }

    private void applyProcessVars(ComplainantRelatedInformation info, Map<String, Object> vars, boolean isManual) {
        Map<String, Object> complaintObj = asStringObjectMap(vars.get(KEY_COMPLAINT));
        Map<String, Object> customerObj = asStringObjectMap(vars.get(KEY_CUSTOMER));

        String custName = nestedOrVar(customerObj, vars, "name", "customerName");
        String accNo = firstNonBlank(
                nestedOrVar(customerObj, vars, KEY_ACCOUNT_NUMBER, KEY_ACCOUNT_NUMBER),
                nestedOrVar(customerObj, vars, KEY_ACCOUNT_NUMBER_SNAKE, KEY_ACCOUNT_NUMBER_SNAKE));
        String phone = firstNonBlank(
                nestedOrVar(customerObj, vars, CustomerContactPhones.CURRENT_CONTACT_PHONE,
                        CustomerContactPhones.CURRENT_CONTACT_PHONE),
                nestedOrVar(customerObj, vars, KEY_PHONE, KEY_PHONE),
                nestedOrVar(customerObj, vars, KEY_PREFERRED_CONTACT_NUMBER, KEY_PREFERRED_CONTACT_NUMBER));
        String email = nestedOrVar(customerObj, vars, KEY_EMAIL, KEY_EMAIL);
        String category = nestedOrVar(complaintObj, vars, "category", "complaintCategory");
        String serviceType = nestedOrVar(complaintObj, vars, KEY_SERVICE_TYPE, KEY_SERVICE_TYPE);
        String description = nestedOrVar(complaintObj, vars, "description", "complaintDescription");
        String district = LocationKeys.complaintDistrict(vars, complaintObj);
        String channel = nestedOrVar(complaintObj, vars, "channel", "channel");
        String evidence = nestedOrVar(complaintObj, vars, KEY_EVIDENCE_NAME, KEY_EVIDENCE_NAME);

        String assignee = firstVar(vars, "assignee", "assignedUser");
        String forwardedTo = firstNonBlank(firstVar(vars, "department", KEY_BRANCH),
                LocationKeys.complaintBranch(vars, complaintObj));

        if (!isManual) {
            setIfNotNull(info::setNameOfComplainant, custName);
            if (accNo != null) {
                info.setAccountNo(accNo);
            }
            String contact = formatContactAddress(phone, email);
            if (contact != null) {
                info.setContactAddress(contact);
            }
            setIfNotNull(info::setComplaintsCategory, category);
            setIfNotNull(info::setServiceType, serviceType);
            setIfNotNull(info::setDetailsOfComplaint, description);
            setIfNotNull(info::setDistrictDepartment, district);
            setIfNotNull(info::setComplaintMadeOnChannel, channel);
            setIfNotNull(info::setSupportingEvidence, evidence);
            setIfNotNull(info::setReceivedBy, firstVar(vars, KEY_RECEIVED_BY, KEY_RECEIVED_BY_SNAKE));
            setIfNotNull(info::setComplaintClassification,
                    firstNonBlank(firstVar(vars, KEY_COMPLAINT_CLASSIFICATION, KEY_PRIORITY_LEVEL),
                            firstVar(vars, "priority", KEY_COMPLAINT_CLASSIFICATION_SNAKE)));
            setIfNotNull(info::setCaseAssignedTo, assignee);
            setIfNotNull(info::setCaseForwardedTo, forwardedTo);
        }

        info.setCaseStatus(OverallComplaintStatus.resolve(vars, false));

        String resolutionNotes = firstVar(vars, "resolutionDetails", "fcrNotes");
        if (resolutionNotes == null) {
            resolutionNotes = (String) vars.get("actionTaken");
        }
        if (resolutionNotes != null
                && (!isManual || info.getResolutionPlan() == null || "-".equals(info.getResolutionPlan()))) {
            info.setResolutionPlan(resolutionNotes);
            info.setLatestStatusAndRemark(resolutionNotes);
        }
    }

    private void setIfNotNull(Consumer<String> setter, String value) {
        if (value != null) {
            setter.accept(value);
        }
    }

    private void hydrateComplaintIdentityFields(ComplainantRelatedInformation info, ComplaintSlaMetrics metrics) {
        Map<String, Object> vars = loadProcessVariables(metrics != null ? metrics.getProcessInstanceId() : null);
        Map<String, Object> customer = asStringObjectMap(vars.get(KEY_CUSTOMER));
        Map<String, Object> complaint = asStringObjectMap(vars.get(KEY_COMPLAINT));

        String account = firstNonBlank(
                text(info.getAccountNo()),
                nestedOrVar(customer, vars, KEY_ACCOUNT_NUMBER, KEY_ACCOUNT_NUMBER),
                nestedOrVar(customer, vars, KEY_ACCOUNT_NUMBER_SNAKE, KEY_ACCOUNT_NUMBER_SNAKE));
        String phone = firstNonBlank(
                nestedOrVar(customer, vars, CustomerContactPhones.CURRENT_CONTACT_PHONE,
                        CustomerContactPhones.CURRENT_CONTACT_PHONE),
                nestedOrVar(customer, vars, KEY_PHONE, KEY_PHONE),
                nestedOrVar(customer, vars, KEY_PREFERRED_CONTACT_NUMBER, KEY_PREFERRED_CONTACT_NUMBER));
        String email = firstNonBlank(nestedOrVar(customer, vars, KEY_EMAIL, KEY_EMAIL));
        String receivedBy = firstNonBlank(
                nestedOrVar(complaint, vars, KEY_RECEIVED_BY, KEY_RECEIVED_BY),
                nestedOrVar(complaint, vars, KEY_RECEIVED_BY_SNAKE, KEY_RECEIVED_BY_SNAKE));
        String serviceType = firstNonBlank(
                nestedOrVar(complaint, vars, KEY_SERVICE_TYPE, KEY_SERVICE_TYPE),
                nestedOrVar(complaint, vars, KEY_SERVICE_TYPE_SNAKE, KEY_SERVICE_TYPE_SNAKE));
        String details = firstNonBlank(
                nestedOrVar(complaint, vars, "description", "complaintDescription"),
                nestedOrVar(complaint, vars, "complaintDetail", "complaint_detail"),
                nestedOrVar(complaint, vars, KEY_DETAILS_OF_COMPLAINT, KEY_DETAILS_OF_COMPLAINT));
        String classification = firstNonBlank(
                nestedOrVar(complaint, vars, KEY_COMPLAINT_CLASSIFICATION, KEY_COMPLAINT_CLASSIFICATION),
                nestedOrVar(complaint, vars, KEY_PRIORITY_LEVEL, KEY_PRIORITY_LEVEL),
                firstVar(vars, "priority", KEY_COMPLAINT_CLASSIFICATION_SNAKE),
                metrics != null ? metrics.getPriority() : null);
        String evidence = firstNonBlank(
                nestedOrVar(complaint, vars, KEY_EVIDENCE_NAME, KEY_EVIDENCE_NAME),
                nestedOrVar(complaint, vars, "evidenceUrl", "evidenceUrl"),
                nestedOrVar(complaint, vars, "voiceName", "voiceName"));

        Map<String, String> fromComplaints = lookupComplaintsIdentity(metrics);
        account = firstNonBlank(account, fromComplaints.get("account"));
        phone = firstNonBlank(phone, fromComplaints.get(KEY_PHONE));
        receivedBy = firstNonBlank(receivedBy, fromComplaints.get(KEY_RECEIVED_BY));
        serviceType = firstNonBlank(serviceType, fromComplaints.get(KEY_SERVICE_TYPE));
        details = firstNonBlank(details, fromComplaints.get("details"));
        classification = firstNonBlank(classification, fromComplaints.get(KEY_CLASSIFICATION));

        fillMissing(info::setAccountNo, info.getAccountNo(), account);
        String contact = formatContactAddress(phone, email);
        if (contact != null && (isMissing(info.getContactAddress())
                || isRicherContact(info.getContactAddress(), contact))) {
            info.setContactAddress(contact);
        }
        fillMissing(info::setReceivedBy, info.getReceivedBy(), receivedBy);
        fillMissing(info::setServiceType, info.getServiceType(), serviceType);
        fillMissing(info::setDetailsOfComplaint, info.getDetailsOfComplaint(), details);
        fillMissing(info::setComplaintClassification, info.getComplaintClassification(), classification);
        fillMissing(info::setSupportingEvidence, info.getSupportingEvidence(), evidence);
    }

    private Map<String, Object> loadProcessVariables(String processInstanceId) {
        if (processInstanceId == null || processInstanceId.isBlank()) {
            return Map.of();
        }
        if (runtimeService != null) {
            try {
                Map<String, Object> vars = runtimeService.getVariables(processInstanceId);
                if (vars != null && !vars.isEmpty()) {
                    return vars;
                }
            } catch (Exception ignored) {
                log.debug("Runtime variables unavailable for {}: {}", processInstanceId, ignored.getMessage());
            }
        }
        if (historyService != null) {
            try {
                var historic = historyService.createHistoricProcessInstanceQuery()
                        .processInstanceId(processInstanceId)
                        .includeProcessVariables()
                        .singleResult();
                if (historic != null && historic.getProcessVariables() != null) {
                    return historic.getProcessVariables();
                }
            } catch (Exception ignored) {
                log.debug("Historic variables unavailable for {}: {}", processInstanceId, ignored.getMessage());
            }
        }
        return Map.of();
    }

    private Map<String, String> lookupComplaintsIdentity(ComplaintSlaMetrics metrics) {
        Map<String, String> result = new HashMap<>();
        if (jdbcTemplate == null || metrics == null) {
            return result;
        }
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT account_number, preferred_contact_number, received_by, service_type, "
                            + "complaint_detail, complaint_classification FROM complaints "
                            + "WHERE ticket_number = ? OR general_ticket_id = ? OR ticket_number = ? OR general_ticket_id = ?",
                    metrics.getComplaintId(), metrics.getComplaintId(),
                    metrics.getGeneralTicketId(), metrics.getGeneralTicketId());
            if (!rows.isEmpty()) {
                Map<String, Object> row = rows.get(0);
                result.put("account", text(row.get(KEY_ACCOUNT_NUMBER_SNAKE)));
                result.put(KEY_PHONE, text(row.get("preferred_contact_number")));
                result.put(KEY_RECEIVED_BY, text(row.get(KEY_RECEIVED_BY_SNAKE)));
                result.put(KEY_SERVICE_TYPE, text(row.get(KEY_SERVICE_TYPE_SNAKE)));
                result.put("details", text(row.get("complaint_detail")));
                result.put(KEY_CLASSIFICATION, text(row.get(KEY_COMPLAINT_CLASSIFICATION_SNAKE)));
            }
        } catch (Exception e) {
            log.debug("Complaints identity lookup skipped: {}", e.getMessage());
        }
        return result;
    }

    private static void fillMissing(Consumer<String> setter, String current, String candidate) {
        if (isMissing(current) && text(candidate) != null) {
            setter.accept(text(candidate));
        }
    }

    private static String formatContactAddress(String phone, String email) {
        String p = text(phone);
        String e = text(email);
        if (p != null && e != null && !p.equalsIgnoreCase(e)) {
            return p + ", " + e;
        }
        return p != null ? p : e;
    }

    private static boolean isMissing(String value) {
        return text(value) == null;
    }

    private static boolean isRicherContact(String existing, String candidate) {
        String current = text(existing);
        String next = text(candidate);
        return current != null && next != null && next.contains(",") && !current.contains(",");
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String trimmed = text(value);
            if (trimmed != null) {
                return trimmed;
            }
        }
        return null;
    }

    private static String text(Object value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.toString().trim();
        if (trimmed.isEmpty() || "-".equals(trimmed) || "null".equalsIgnoreCase(trimmed)
                || "undefined".equalsIgnoreCase(trimmed)) {
            return null;
        }
        return trimmed;
    }

    @Transactional
    public ComplainantRelatedInformation syncComplaintRecord(String ticketId, Map<String, Object> vars) {
        if (ticketId == null || ticketId.trim().isEmpty()) {
            return null;
        }

        Optional<ComplainantRelatedInformation> opt = repository.findByUniqueIdNo(ticketId);
        ComplainantRelatedInformation info = opt.orElseGet(() -> {
            ComplainantRelatedInformation n = new ComplainantRelatedInformation();
            n.setUniqueIdNo(ticketId);
            n.setIsManuallyEdited(false);
            return n;
        });

        boolean isManual = Boolean.TRUE.equals(info.getIsManuallyEdited());
        if (vars != null) {
            applyProcessVars(info, vars, isManual);
        }

        if (info.getDateOfComplaint() == null) {
            info.setDateOfComplaint(LocalDateTime.now(SYSTEM_ZONE));
        }

        info.setResolutionTimeWorkingDays(
                calculateWorkingDays(info.getDateOfComplaint(), info.getActualResolutionDate()));
        applyDefaultsIfMissing(info);

        return repository.save(info);
    }

    private void applyDefaultsIfMissing(ComplainantRelatedInformation info) {
        info.setNameOfComplainant(defaultIfBlank(info.getNameOfComplainant()));
        info.setAccountNo(defaultIfBlank(info.getAccountNo()));
        info.setContactAddress(defaultIfBlank(info.getContactAddress()));
        info.setCustomerSegment(defaultIfBlank(info.getCustomerSegment()));
        info.setReceivedBy(defaultIfBlank(info.getReceivedBy()));
        info.setComplaintMadeOnChannel(defaultIfBlank(info.getComplaintMadeOnChannel()));
        info.setSpecificChannelName(defaultIfBlank(info.getSpecificChannelName()));
        info.setDistrictDepartment(defaultIfBlank(info.getDistrictDepartment()));
        info.setServiceType(defaultIfBlank(info.getServiceType()));
        info.setDetailsOfComplaint(defaultIfBlank(info.getDetailsOfComplaint()));
        info.setComplaintClassification(defaultIfBlank(info.getComplaintClassification()));
        info.setSupportingEvidence(defaultIfBlank(info.getSupportingEvidence()));
        info.setComplainantAcknowledged(defaultIfBlank(info.getComplainantAcknowledged()));
        info.setComplaintJustified(defaultIfBlank(info.getComplaintJustified()));
        info.setValidityReasonForJustifiedComplaints(defaultIfBlank(info.getValidityReasonForJustifiedComplaints()));
        info.setNatureOfComplaints(defaultIfBlank(info.getNatureOfComplaints()));
        info.setComplaintsCategory(defaultIfBlank(info.getComplaintsCategory()));
        info.setCaseAssignedTo(defaultIfBlank(info.getCaseAssignedTo()));
        info.setCaseForwardedTo(defaultIfBlank(info.getCaseForwardedTo()));
        info.setReasonForForwarding(defaultIfBlank(info.getReasonForForwarding()));
        info.setEscalatedTo(defaultIfBlank(info.getEscalatedTo()));
        info.setReasonForEscalation(defaultIfBlank(info.getReasonForEscalation()));
        info.setResolutionPlan(defaultIfBlank(info.getResolutionPlan()));
        info.setResolutionOutcomeNotified(defaultIfBlank(info.getResolutionOutcomeNotified()));
        info.setMeansOfNotification(defaultIfBlank(info.getMeansOfNotification()));
        info.setComplainantAcknowledgedResolution(defaultIfBlank(info.getComplainantAcknowledgedResolution()));
        info.setAdviceGivenToComplainant(defaultIfBlank(info.getAdviceGivenToComplainant()));
        info.setCustomerReactionToHandlingProcess(defaultIfBlank(info.getCustomerReactionToHandlingProcess()));
        info.setCustomerLifetimeValue(defaultIfBlank(info.getCustomerLifetimeValue()));
        info.setRemarkAndSpecialNote(defaultIfBlank(info.getRemarkAndSpecialNote()));
        info.setRequiresFollowUp(defaultIfBlank(info.getRequiresFollowUp()));
        info.setLatestStatusAndRemark(defaultIfBlank(info.getLatestStatusAndRemark()));
        if (info.getCaseStatus() == null || info.getCaseStatus().trim().isEmpty()) {
            info.setCaseStatus("RECORDED");
        }
        if (info.getResolutionTimeWorkingDays() == null) {
            info.setResolutionTimeWorkingDays(0);
        }
        if (info.getIsManuallyEdited() == null) {
            info.setIsManuallyEdited(false);
        }
    }

    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void syncAllExistingComplaints() {
        try {
            List<ComplaintSlaMetrics> metricsList = slaMetricsRepository.findAll();
            metricsList.forEach(this::syncMetricQuietly);
        } catch (Exception e) {
            log.warn("Note: syncAllExistingComplaints startup sync: {}", e.getMessage());
        }
    }

    private void syncMetricQuietly(ComplaintSlaMetrics metrics) {
        try {
            syncFromSlaMetrics(metrics);
        } catch (Exception e) {
            log.error("Error syncing metric: {} -> {}", metrics.getComplaintId(), e.getMessage());
        }
    }

    private void syncClassifiedComplaintsIntoLedger() {
        try {
            slaMetricsRepository.findAll().stream()
                    .filter(SlaTrackingService::isClassifiedComplaint)
                    .forEach(this::syncMetricQuietly);
        } catch (Exception e) {
            log.warn("Could not sync classified complaints into CRI ledger: {}", e.getMessage());
        }
    }

    public Page<ComplainantRelatedInformation> searchAndFilter(
            String search, String status, String category, String district, int page, int size) {

        // Keep the CRI ledger aligned with classified SLA complaints (any official
        // status). New ON_TRACK / ESCALATED cases must appear without a restart.
        syncClassifiedComplaintsIntoLedger();

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, KEY_UNIQUE_ID_NO));

        Specification<ComplainantRelatedInformation> spec = (root, query, cb) -> {
            var predicate = cb.conjunction();

            if (category != null && !category.trim().isEmpty() && !"ALL".equalsIgnoreCase(category.trim())) {
                predicate = cb.and(predicate, cb.equal(root.get("complaintsCategory"), category.trim()));
            }

            if (district != null && !district.trim().isEmpty() && !"ALL".equalsIgnoreCase(district.trim())) {
                predicate = cb.and(predicate, cb.equal(root.get("districtDepartment"), district.trim()));
            }

            if (search != null && !search.trim().isEmpty()) {
                String q = "%" + search.trim().toLowerCase() + "%";
                var searchPred = cb.or(
                        cb.like(cb.lower(root.get(KEY_UNIQUE_ID_NO)), q),
                        cb.like(cb.lower(root.get("nameOfComplainant")), q),
                        cb.like(cb.lower(root.get("accountNo")), q),
                        cb.like(cb.lower(root.get(KEY_DETAILS_OF_COMPLAINT)), q),
                        cb.like(cb.lower(root.get("caseAssignedTo")), q));
                predicate = cb.and(predicate, searchPred);
            }

            return predicate;
        };

        // Overlay live status first, then filter, so the status the user sees is
        // the same status used to include or exclude the row.
        List<ComplainantRelatedInformation> all = new ArrayList<>(repository.findAll(spec,
                Sort.by(Sort.Direction.ASC, KEY_UNIQUE_ID_NO)));
        all.forEach(this::overlayLiveCaseStatus);
        all = all.stream().filter(this::isClassifiedComplaintRecord).collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        String statusFilter = status != null ? status.trim() : "";
        if (!statusFilter.isEmpty() && !"ALL".equalsIgnoreCase(statusFilter)) {
            all = all.stream()
                    .filter(item -> statusFilter.equalsIgnoreCase(item.getCaseStatus()))
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        }

        all.sort(Comparator.comparing(ComplainantRelatedInformation::getUniqueIdNo, TicketNumberSort.ASC));

        int fromIndex = Math.min(page * size, all.size());
        int toIndex = Math.min(fromIndex + size, all.size());
        return new org.springframework.data.domain.PageImpl<>(all.subList(fromIndex, toIndex), pageable, all.size());
    }

    /**
     * Classified CRI rows for reporting modules. Reuses the same population rule as
     * the CRI grid (OTHER and INTAKE-only excluded).
     */
    public List<ComplainantRelatedInformation> listClassifiedComplaints() {
        return searchAndFilter(null, "ALL", "ALL", "ALL", 0, 10_000).getContent();
    }

    public Optional<ComplainantRelatedInformation> findById(Long id) {
        return repository.findById(id);
    }

    public Optional<ComplainantRelatedInformation> findByUniqueIdNo(String uniqueIdNo) {
        return repository.findByUniqueIdNo(uniqueIdNo);
    }

    public void applyDisplayedCaseStatus(ComplainantRelatedInformation info) {
        overlayLiveCaseStatus(info);
    }

    private boolean isClassifiedComplaintRecord(ComplainantRelatedInformation info) {
        if (info == null) {
            return false;
        }
        String ticket = info.getUniqueIdNo();
        Optional<ComplaintSlaMetrics> sla = ticket == null ? Optional.empty()
                : slaMetricsRepository.findByComplaintId(ticket);
        if (sla.isEmpty() && ticket != null) {
            sla = slaMetricsRepository.findByGeneralTicketId(ticket);
        }
        if (sla.isEmpty() && ticket != null) {
            sla = slaMetricsRepository.findByDbcTicketId(ticket);
        }
        if (sla.isPresent()) {
            return SlaTrackingService.isClassifiedComplaint(sla.get());
        }
        if ("OTHER".equalsIgnoreCase(info.getCaseStatus()) || "INTAKE".equalsIgnoreCase(info.getCaseStatus())) {
            return false;
        }
        return ticket != null && (ticket.startsWith("DBC-") || ticket.startsWith("FCR-"));
    }

    @Transactional
    public ComplainantRelatedInformation updateRecord(Long id, ComplainantRelatedInformation updated, String username) {
        ComplainantRelatedInformation existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Record not found with ID: " + id));

        copyEditableTextFields(existing, updated);
        copyEditableDateFields(existing, updated);

        if (updated.getCaseStatus() != null && !updated.getCaseStatus().trim().isEmpty()) {
            existing.setCaseStatus(normalizeStatus(updated.getCaseStatus()));
        }

        existing.setResolutionTimeWorkingDays(
                calculateWorkingDays(existing.getDateOfComplaint(), existing.getActualResolutionDate()));

        existing.setIsManuallyEdited(true);
        existing.setLastEditedBy(username != null ? username : "Admin");
        existing.setLastEditedAt(LocalDateTime.now(SYSTEM_ZONE));

        ComplainantRelatedInformation saved = repository.save(existing);
        saveUpdateAuditLog(existing);
        return saved;
    }

    private void copyText(Consumer<String> setter, String value) {
        if (value != null) {
            setter.accept(defaultIfBlank(value));
        }
    }

    private void copyDate(Consumer<LocalDateTime> setter, LocalDateTime value) {
        if (value != null) {
            setter.accept(value);
        }
    }

    private void copyEditableTextFields(ComplainantRelatedInformation existing, ComplainantRelatedInformation updated) {
        copyText(existing::setNameOfComplainant, updated.getNameOfComplainant());
        copyText(existing::setAccountNo, updated.getAccountNo());
        copyText(existing::setContactAddress, updated.getContactAddress());
        copyText(existing::setCustomerSegment, updated.getCustomerSegment());
        copyText(existing::setReceivedBy, updated.getReceivedBy());
        copyText(existing::setComplaintMadeOnChannel, updated.getComplaintMadeOnChannel());
        copyText(existing::setSpecificChannelName, updated.getSpecificChannelName());
        copyText(existing::setDistrictDepartment, updated.getDistrictDepartment());
        copyText(existing::setServiceType, updated.getServiceType());
        copyText(existing::setDetailsOfComplaint, updated.getDetailsOfComplaint());
        copyText(existing::setComplaintClassification, updated.getComplaintClassification());
        copyText(existing::setSupportingEvidence, updated.getSupportingEvidence());
        copyText(existing::setComplainantAcknowledged, updated.getComplainantAcknowledged());
        copyText(existing::setComplaintJustified, updated.getComplaintJustified());
        copyText(existing::setValidityReasonForJustifiedComplaints, updated.getValidityReasonForJustifiedComplaints());
        copyText(existing::setNatureOfComplaints, updated.getNatureOfComplaints());
        copyText(existing::setComplaintsCategory, updated.getComplaintsCategory());
        copyText(existing::setCaseAssignedTo, updated.getCaseAssignedTo());
        copyText(existing::setCaseForwardedTo, updated.getCaseForwardedTo());
        copyText(existing::setReasonForForwarding, updated.getReasonForForwarding());
        copyText(existing::setEscalatedTo, updated.getEscalatedTo());
        copyText(existing::setReasonForEscalation, updated.getReasonForEscalation());
        copyText(existing::setResolutionPlan, updated.getResolutionPlan());
        copyText(existing::setResolutionOutcomeNotified, updated.getResolutionOutcomeNotified());
        copyText(existing::setMeansOfNotification, updated.getMeansOfNotification());
        copyText(existing::setComplainantAcknowledgedResolution, updated.getComplainantAcknowledgedResolution());
        copyText(existing::setAdviceGivenToComplainant, updated.getAdviceGivenToComplainant());
        copyText(existing::setCustomerReactionToHandlingProcess, updated.getCustomerReactionToHandlingProcess());
        copyText(existing::setCustomerLifetimeValue, updated.getCustomerLifetimeValue());
        copyText(existing::setRemarkAndSpecialNote, updated.getRemarkAndSpecialNote());
        copyText(existing::setRequiresFollowUp, updated.getRequiresFollowUp());
        copyText(existing::setLatestStatusAndRemark, updated.getLatestStatusAndRemark());
    }

    private void copyEditableDateFields(ComplainantRelatedInformation existing, ComplainantRelatedInformation updated) {
        copyDate(existing::setDateOfComplaint, updated.getDateOfComplaint());
        copyDate(existing::setExpectedResolutionDate, updated.getExpectedResolutionDate());
        copyDate(existing::setActualResolutionDate, updated.getActualResolutionDate());
        copyDate(existing::setDateCaseForwarded, updated.getDateCaseForwarded());
        copyDate(existing::setDateOfEscalation, updated.getDateOfEscalation());
    }

    private void saveUpdateAuditLog(ComplainantRelatedInformation existing) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .action("UPDATE_COMPLAINANT_INFORMATION")
                    .actor(existing.getLastEditedBy())
                    .complaintId(existing.getUniqueIdNo())
                    .customerName(existing.getNameOfComplainant())
                    .complaintCategory(existing.getComplaintsCategory())
                    .description("Updated complainant related information record for ID: " + existing.getUniqueIdNo())
                    .createdAt(LocalDateTime.now(SYSTEM_ZONE))
                    .build();
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to log audit for update: {}", e.getMessage());
        }
    }

    @Transactional
    public void deleteRecord(Long id) {
        repository.deleteById(id);
    }
}
