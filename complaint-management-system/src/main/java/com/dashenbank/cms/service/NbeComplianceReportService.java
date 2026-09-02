package com.dashenbank.cms.service;

import com.dashenbank.cms.model.ComplainantRelatedInformation;
import com.dashenbank.cms.model.ComplaintSlaMetrics;
import com.dashenbank.cms.model.NbeComplianceReport;
import com.dashenbank.cms.repository.ComplaintSlaMetricsRepository;
import com.dashenbank.cms.repository.NbeComplianceReportRepository;
import com.dashenbank.cms.util.TicketNumberSort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Persistence for NBE Annex 1 / Annex 2 regulatory fields only.
 *
 * <p>
 * Writes go to {@code nbe_compliance_reports}. Customer and complaint identity
 * is always loaded from {@code complainant_related_information} and
 * {@code complaint_sla_metrics}. Flowable variables are never the write target.
 */
@Service
public class NbeComplianceReportService {

    private static final Logger log = LoggerFactory.getLogger(NbeComplianceReportService.class);
    private static final ZoneId SYSTEM_ZONE = ZoneId.of("Africa/Addis_Ababa");
    private static final DateTimeFormatter ISO_LOCAL = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static final String KEY_COMPLAINT_ID = "complaintId";
    private static final String KEY_DBC_TICKET_ID = "dbcTicketId";
    private static final String KEY_PROCESS_INSTANCE_ID = "processInstanceId";
    private static final String KEY_CLASSIFICATION = "classification";
    private static final String KEY_STATUS = "status";
    private static final String KEY_MOBILE = "mobile";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_LODGED_DATE = "lodgedDate";
    private static final String KEY_RESOLVED_DATE = "resolvedDate";
    private static final String KEY_OVERALL_STATUS = "overallStatus";
    private static final String CLASSIFICATION_COMPLAINT = "COMPLAINT";
    private static final String CLASSIFICATION_DECLINED = "DECLINED";
    private static final String CLASSIFICATION_OTHER = "OTHER";
    private static final String CLASSIFICATION_INTAKE = "INTAKE";
    private static final String PREFIX_DBC = "DBC-";
    private static final String PREFIX_FCR = "FCR-";

    private final NbeComplianceReportRepository repository;
    private final ComplainantRelatedInformationService criService;
    private final ComplaintSlaMetricsRepository slaMetricsRepository;

    @Autowired
    public NbeComplianceReportService(NbeComplianceReportRepository repository,
            ComplainantRelatedInformationService criService,
            ComplaintSlaMetricsRepository slaMetricsRepository) {
        this.repository = repository;
        this.criService = criService;
        this.slaMetricsRepository = slaMetricsRepository;
    }

    public List<NbeComplianceReport> findAll() {
        return repository.findAll();
    }

    public Optional<NbeComplianceReport> findByTicketNumber(String ticketNumber) {
        if (isBlank(ticketNumber)) {
            return Optional.empty();
        }
        return repository.findByTicketNumber(ticketNumber.trim());
    }

    public Map<String, NbeComplianceReport> findAllByTicketNumber() {
        Map<String, NbeComplianceReport> byTicket = new HashMap<>();
        for (NbeComplianceReport report : repository.findAll()) {
            if (!isBlank(report.getTicketNumber())) {
                byTicket.put(report.getTicketNumber().trim(), report);
            }
        }
        return byTicket;
    }

    /**
     * Creates or updates NBE-specific fields for a ticket. Null payload fields are
     * left unchanged; blank strings clear a previously stored value.
     *
     * @throws IllegalArgumentException when ticketNumber is missing or no CRI row
     *                                  exists for that ticket
     */
    @Transactional
    public NbeComplianceReport upsertByTicketNumber(NbeComplianceReport incoming, String username) {
        if (incoming == null || isBlank(incoming.getTicketNumber())) {
            throw new IllegalArgumentException("ticketNumber is required to save an NBE compliance report");
        }

        String ticketNumber = incoming.getTicketNumber().trim();
        if (!criService.findByUniqueIdNo(ticketNumber).isPresent()) {
            throw new IllegalArgumentException(
                    "No complainant related information record exists for ticket " + ticketNumber);
        }
        Optional<ComplaintSlaMetrics> sla = findSlaByTicket(ticketNumber);
        if (sla.isPresent() && !SlaTrackingService.isClassifiedComplaint(sla.get())) {
            throw new IllegalArgumentException(
                    "Ticket " + ticketNumber + " is not a classified complaint and cannot be stored as an NBE report");
        }

        NbeComplianceReport target = repository.findByTicketNumber(ticketNumber)
                .orElseGet(() -> NbeComplianceReport.builder().ticketNumber(ticketNumber).build());

        applyIfPresent(target::setProcessInstanceId, incoming.getProcessInstanceId());
        applyIfPresent(target::setReportStatus, incoming.getReportStatus());
        applyIfPresent(target::setStaffHandling, incoming.getStaffHandling());
        if (incoming.getDaysOpen() != null) {
            target.setDaysOpen(incoming.getDaysOpen());
        }
        applyIfPresent(target::setReasonForNonResolution, incoming.getReasonForNonResolution());
        applyIfPresent(target::setAdditionalComments, incoming.getAdditionalComments());

        target.setUpdatedBy(isBlank(username) ? "Admin" : username);

        NbeComplianceReport saved = repository.save(target);
        log.info("Persisted NBE compliance report for ticket {} (id {})", saved.getTicketNumber(), saved.getId());
        return saved;
    }

    @Transactional
    public boolean deleteByTicketNumber(String ticketNumber) {
        Optional<NbeComplianceReport> existing = findByTicketNumber(ticketNumber);
        if (existing.isEmpty()) {
            return false;
        }
        repository.delete(existing.get());
        return true;
    }

    /**
     * Hydrates operational columns from CRI/SLA, overlays stored NBE fields, and
     * appends stored reports whose Flowable history has been purged.
     */
    public List<Map<String, Object>> mergeWithOperationalSources(List<Map<String, Object>> historicRows) {
        List<Map<String, Object>> historic = historicRows != null ? historicRows : List.of();
        Map<String, NbeComplianceReport> storedByTicket = findAllByTicketNumber();
        Set<String> seenTickets = new HashSet<>();
        List<Map<String, Object>> classifiedRows = new ArrayList<>();

        appendHistoricClassifiedRows(historic, storedByTicket, seenTickets, classifiedRows);
        appendUnseenStoredReports(storedByTicket, seenTickets, classifiedRows);
        appendUnseenSlaMetrics(storedByTicket, seenTickets, classifiedRows);
        classifiedRows.sort(classifiedRowOrder());
        return classifiedRows;
    }

    private void appendHistoricClassifiedRows(List<Map<String, Object>> historic,
            Map<String, NbeComplianceReport> storedByTicket, Set<String> seenTickets,
            List<Map<String, Object>> classifiedRows) {
        for (Map<String, Object> row : historic) {
            hydrateIdentityFromOperationalStores(row);
            if (!isClassifiedComplaintRow(row)) {
                continue;
            }
            overlayOnReportRow(row, resolveStoredReport(row, storedByTicket));
            String ticket = firstTicket(row);
            if (!isBlank(ticket)) {
                seenTickets.add(ticket);
            }
            classifiedRows.add(row);
        }
    }

    private void appendUnseenStoredReports(Map<String, NbeComplianceReport> storedByTicket, Set<String> seenTickets,
            List<Map<String, Object>> classifiedRows) {
        for (NbeComplianceReport stored : storedByTicket.values()) {
            appendStoredIfUnseen(stored, seenTickets, classifiedRows);
        }
    }

    private void appendStoredIfUnseen(NbeComplianceReport stored, Set<String> seenTickets,
            List<Map<String, Object>> classifiedRows) {
        if (seenTickets.contains(stored.getTicketNumber())) {
            return;
        }
        Map<String, Object> row = identityRow(stored.getTicketNumber(), stored.getProcessInstanceId());
        hydrateIdentityFromOperationalStores(row);
        if (!isClassifiedComplaintRow(row)) {
            return;
        }
        overlayOnReportRow(row, stored);
        seenTickets.add(stored.getTicketNumber().trim());
        classifiedRows.add(row);
    }

    private void appendUnseenSlaMetrics(Map<String, NbeComplianceReport> storedByTicket, Set<String> seenTickets,
            List<Map<String, Object>> classifiedRows) {
        for (ComplaintSlaMetrics metrics : slaMetricsRepository.findAll()) {
            appendSlaIfUnseen(metrics, storedByTicket, seenTickets, classifiedRows);
        }
    }

    private void appendSlaIfUnseen(ComplaintSlaMetrics metrics, Map<String, NbeComplianceReport> storedByTicket,
            Set<String> seenTickets, List<Map<String, Object>> classifiedRows) {
        if (!SlaTrackingService.isClassifiedComplaint(metrics)) {
            return;
        }
        String ticket = firstNonBlank(metrics.getDbcTicketId(),
                firstNonBlank(metrics.getComplaintId(), metrics.getGeneralTicketId()));
        if (isBlank(ticket) || seenTickets.contains(ticket.trim())) {
            return;
        }
        Map<String, Object> row = identityRow(ticket, metrics.getProcessInstanceId());
        hydrateIdentityFromOperationalStores(row);
        if (!isClassifiedComplaintRow(row)) {
            return;
        }
        overlayOnReportRow(row, storedByTicket.get(ticket.trim()));
        seenTickets.add(ticket.trim());
        classifiedRows.add(row);
    }

    private Map<String, Object> identityRow(String ticket, String processInstanceId) {
        Map<String, Object> row = new HashMap<>();
        row.put(KEY_COMPLAINT_ID, ticket);
        row.put(KEY_DBC_TICKET_ID, ticket);
        row.put(KEY_PROCESS_INSTANCE_ID, processInstanceId);
        return row;
    }

    private Comparator<Map<String, Object>> classifiedRowOrder() {
        return Comparator.comparing(row -> {
            String dbc = stringValue(row.get(KEY_DBC_TICKET_ID));
            if (dbc.startsWith(PREFIX_DBC) || dbc.startsWith(PREFIX_FCR)) {
                return dbc;
            }
            return firstTicket(row);
        }, TicketNumberSort.ASC);
    }

    private boolean isClassifiedComplaintRow(Map<String, Object> row) {
        String classification = stringValue(row.get(KEY_CLASSIFICATION));
        String status = stringValue(row.get(KEY_STATUS));
        if (CLASSIFICATION_OTHER.equalsIgnoreCase(classification)
                || CLASSIFICATION_INTAKE.equalsIgnoreCase(classification)) {
            return false;
        }
        if (CLASSIFICATION_OTHER.equalsIgnoreCase(status)
                && !CLASSIFICATION_COMPLAINT.equalsIgnoreCase(classification)
                && !CLASSIFICATION_DECLINED.equalsIgnoreCase(classification)) {
            return false;
        }
        String ticket = firstTicket(row);
        return ticket.startsWith(PREFIX_DBC) || ticket.startsWith(PREFIX_FCR)
                || CLASSIFICATION_DECLINED.equalsIgnoreCase(classification)
                || CLASSIFICATION_DECLINED.equalsIgnoreCase(status)
                || CLASSIFICATION_COMPLAINT.equalsIgnoreCase(classification);
    }

    /**
     * Overlays stored NBE regulatory fields. Does not copy reportStatus onto
     * overallStatus. Blank stored strings are applied so comments can be cleared.
     */
    public void overlayOnReportRow(Map<String, Object> row, NbeComplianceReport stored) {
        if (row == null || stored == null) {
            return;
        }
        overlayString(row, "staffHandling", stored.getStaffHandling());
        overlayString(row, "reasonForNonResolution", stored.getReasonForNonResolution());
        overlayString(row, "additionalComments", stored.getAdditionalComments());
        overlayString(row, "reportStatus", stored.getReportStatus());
        if (stored.getDaysOpen() != null) {
            row.put("daysOpen", stored.getDaysOpen());
        }
        row.put("nbeReportId", stored.getId());
        row.put("nbeReportUpdatedAt", stored.getUpdatedAt());
        row.put("nbeReportUpdatedBy", stored.getUpdatedBy());
    }

    public NbeComplianceReport resolveStoredReport(Map<String, Object> row,
            Map<String, NbeComplianceReport> storedByTicket) {
        if (row == null || storedByTicket == null || storedByTicket.isEmpty()) {
            return null;
        }
        NbeComplianceReport match = storedByTicket.get(stringValue(row.get(KEY_COMPLAINT_ID)));
        if (match == null) {
            match = storedByTicket.get(stringValue(row.get(KEY_DBC_TICKET_ID)));
        }
        return match;
    }

    private void hydrateIdentityFromOperationalStores(Map<String, Object> row) {
        String ticket = firstTicket(row);
        Optional<ComplainantRelatedInformation> criOpt = isBlank(ticket) ? Optional.empty()
                : criService.findByUniqueIdNo(ticket);
        criOpt.ifPresent(criService::applyDisplayedCaseStatus);
        applyCriIdentity(row, criOpt.orElse(null));
        applySlaIdentity(row, resolveSlaForRow(ticket, row));
        row.putIfAbsent(KEY_CLASSIFICATION, CLASSIFICATION_COMPLAINT);
        row.putIfAbsent("reasonForNonResolution", "");
        row.putIfAbsent("additionalComments", "");
        row.putIfAbsent("reportStatus", "");
        applyPhoneOnlyMobile(row);
    }

    private ComplaintSlaMetrics resolveSlaForRow(String ticket, Map<String, Object> row) {
        Optional<ComplaintSlaMetrics> slaOpt = findSlaByTicket(ticket);
        if (slaOpt.isEmpty() && row.get(KEY_PROCESS_INSTANCE_ID) != null) {
            slaOpt = slaMetricsRepository.findByProcessInstanceId(String.valueOf(row.get(KEY_PROCESS_INSTANCE_ID)));
        }
        return slaOpt.orElse(null);
    }

    private void applyCriIdentity(Map<String, Object> row, ComplainantRelatedInformation cri) {
        if (cri == null) {
            return;
        }
        putIfHasText(row, "complainantName", cri.getNameOfComplainant());
        String[] contact = splitPhoneAndEmail(cri.getContactAddress());
        row.put(KEY_MOBILE, contact[0] != null ? contact[0] : phoneOnly(row.get(KEY_MOBILE)));
        if (contact[1] != null) {
            row.put(KEY_EMAIL, contact[1]);
        }
        putIfHasText(row, "issuesRaised", firstNonBlank(cri.getDetailsOfComplaint(), cri.getComplaintsCategory()));
        if (cri.getDateOfComplaint() != null) {
            row.put(KEY_LODGED_DATE, ISO_LOCAL.format(cri.getDateOfComplaint()));
        }
        if (cri.getActualResolutionDate() != null) {
            row.put(KEY_RESOLVED_DATE, ISO_LOCAL.format(cri.getActualResolutionDate()));
        }
        if (cri.getCaseStatus() != null) {
            row.put(KEY_OVERALL_STATUS, cri.getCaseStatus());
            row.put(KEY_STATUS, cri.getCaseStatus());
        }
    }

    private void applySlaIdentity(Map<String, Object> row, ComplaintSlaMetrics sla) {
        if (sla == null) {
            return;
        }
        putIfBlank(row, "complainantName", sla.getCustomerName());
        putIfBlank(row, "issuesRaised", sla.getComplaintCategory());
        putIfBlank(row, "slaStatus", sla.getSlaStatus());
        if (sla.getClassification() != null) {
            row.putIfAbsent(KEY_CLASSIFICATION, sla.getClassification());
        }
        if (row.get(KEY_LODGED_DATE) == null && sla.getCreatedAt() != null) {
            row.put(KEY_LODGED_DATE, ISO_LOCAL.format(sla.getCreatedAt()));
        }
        if (row.get(KEY_RESOLVED_DATE) == null && sla.getResolvedAt() != null) {
            row.put(KEY_RESOLVED_DATE, ISO_LOCAL.format(sla.getResolvedAt()));
        }
        row.put("daysOpen", calculateDaysOpen(sla.getCreatedAt(), sla.getResolvedAt()));
        if (row.get(KEY_OVERALL_STATUS) == null && sla.getOverallStatus() != null) {
            row.put(KEY_OVERALL_STATUS, sla.getOverallStatus());
            row.put(KEY_STATUS, sla.getOverallStatus());
        }
        if (row.get(KEY_PROCESS_INSTANCE_ID) == null) {
            row.put(KEY_PROCESS_INSTANCE_ID, sla.getProcessInstanceId());
        }
    }

    private Optional<ComplaintSlaMetrics> findSlaByTicket(String ticket) {
        if (isBlank(ticket)) {
            return Optional.empty();
        }
        Optional<ComplaintSlaMetrics> metrics = slaMetricsRepository.findByComplaintId(ticket);
        if (metrics.isEmpty()) {
            metrics = slaMetricsRepository.findByGeneralTicketId(ticket);
        }
        if (metrics.isEmpty()) {
            metrics = slaMetricsRepository.findByDbcTicketId(ticket);
        }
        return metrics;
    }

    private long calculateDaysOpen(LocalDateTime start, LocalDateTime end) {
        if (start == null) {
            return 0;
        }
        LocalDateTime to = end != null ? end : LocalDateTime.now(SYSTEM_ZONE);
        return ChronoUnit.DAYS.between(start.toLocalDate(), to.toLocalDate());
    }

    private void applyIfPresent(Consumer<String> setter, String value) {
        if (value != null) {
            setter.accept(value.trim());
        }
    }

    private void overlayString(Map<String, Object> row, String key, String value) {
        if (value != null) {
            row.put(key, value);
        }
    }

    private void putIfHasText(Map<String, Object> row, String key, String value) {
        if (!isBlank(value)) {
            row.put(key, value);
        }
    }

    private void putIfBlank(Map<String, Object> row, String key, String value) {
        if (isBlank(value)) {
            return;
        }
        Object existing = row.get(key);
        if (existing == null || isBlank(String.valueOf(existing)) || "N/A".equals(String.valueOf(existing))) {
            row.put(key, value);
        }
    }

    private String firstTicket(Map<String, Object> row) {
        String complaintId = stringValue(row.get(KEY_COMPLAINT_ID));
        if (!isBlank(complaintId) && !"N/A".equals(complaintId)) {
            return complaintId;
        }
        return stringValue(row.get(KEY_DBC_TICKET_ID));
    }

    private String firstNonBlank(String primary, String fallback) {
        return isBlank(primary) ? fallback : primary;
    }

    private void applyPhoneOnlyMobile(Map<String, Object> row) {
        String[] parts = splitPhoneAndEmail(stringValue(row.get(KEY_MOBILE)));
        if (parts[0] != null) {
            row.put(KEY_MOBILE, parts[0]);
        } else if (stringValue(row.get(KEY_MOBILE)).contains("@")) {
            row.put(KEY_MOBILE, "");
        }
        if (isBlank(stringValue(row.get(KEY_EMAIL))) && parts[1] != null) {
            row.put(KEY_EMAIL, parts[1]);
        }
    }

    private static String[] splitPhoneAndEmail(String contact) {
        String[] result = new String[] { null, null };
        if (isEmptyContact(contact)) {
            return result;
        }
        StringBuilder phones = new StringBuilder();
        StringBuilder emails = new StringBuilder();
        for (String part : contact.split("[,;/]")) {
            appendContactPart(part.trim(), phones, emails);
        }
        result[0] = phones.length() > 0 ? phones.toString() : null;
        result[1] = emails.length() > 0 ? emails.toString() : null;
        return result;
    }

    private static boolean isEmptyContact(String contact) {
        return contact == null || contact.isBlank() || "N/A".equalsIgnoreCase(contact.trim())
                || "-".equals(contact.trim());
    }

    private static void appendContactPart(String value, StringBuilder phones, StringBuilder emails) {
        if (value.isEmpty()) {
            return;
        }
        StringBuilder target = value.contains("@") ? emails : phones;
        if (target.length() > 0) {
            target.append(", ");
        }
        target.append(value);
    }

    private static String phoneOnly(Object value) {
        return splitPhoneAndEmail(value == null ? "" : String.valueOf(value))[0];
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
