package com.dashenbank.cms.service;

import com.dashenbank.cms.model.HolidayCalendar;
import com.dashenbank.cms.model.SlaConfig;
import com.dashenbank.cms.repository.AuditLogRepository;
import com.dashenbank.cms.repository.ComplaintSlaMetricsRepository;
import com.dashenbank.cms.repository.HolidayCalendarRepository;
import com.dashenbank.cms.repository.SlaConfigRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SlaConfigService {

    private static final Logger log = LoggerFactory.getLogger(SlaConfigService.class);

    private static final String GROUP_INTAKE_SLA = "INTAKE_SLA";
    private static final String GROUP_STAGE_SLA = "STAGE_SLA";
    private static final String GROUP_CUSTOMER_NOTIFICATION = "CUSTOMER_NOTIFICATION";
    private static final String GROUP_OVERALL_SLA = "OVERALL_SLA";
    private static final String GROUP_ESCALATION_SLA = "ESCALATION_SLA";

    private static final String PRIORITY_HIGHLY_SENSITIVE = "HIGHLY_SENSITIVE";
    private static final String PRIORITY_SENSITIVE = "SENSITIVE";
    private static final String PRIORITY_GENERAL = "GENERAL";
    private static final String PRIORITY_ALL = "ALL";

    private static final String TYPE_PUBLIC_HOLIDAY = "PUBLIC_HOLIDAY";

    private final SlaConfigRepository slaConfigRepository;
    private final HolidayCalendarRepository holidayCalendarRepository;
    private final ComplaintSlaMetricsRepository slaMetricsRepository;
    private final AuditLogRepository auditLogRepository;

    public SlaConfigService(SlaConfigRepository slaConfigRepository,
                            HolidayCalendarRepository holidayCalendarRepository,
                            ComplaintSlaMetricsRepository slaMetricsRepository,
                            AuditLogRepository auditLogRepository) {
        this.slaConfigRepository = slaConfigRepository;
        this.holidayCalendarRepository = holidayCalendarRepository;
        this.slaMetricsRepository = slaMetricsRepository;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Legacy config keys retained only so admin-edited minutes migrate onto the
     * canonical matrix keys used by the Java SLA engine.
     */
    private static final Map<String, String> LEGACY_KEY_MIGRATIONS = Map.ofEntries(
            Map.entry("WORKUNIT_RESOLUTION_HS", "RESOLUTION_NO_INVESTIGATION_HS_S"),
            Map.entry("WORKUNIT_RESOLUTION_SENSITIVE", "RESOLUTION_NO_INVESTIGATION_HS_S"),
            Map.entry("WORKUNIT_RESOLUTION_GENERAL", "RESOLUTION_NO_INVESTIGATION_GENERAL"),
            Map.entry("OVERALL_HS_INVESTIGATION", "OVERALL_INVESTIGATION_HS_S"),
            Map.entry("OVERALL_SENSITIVE_INVESTIGATION", "OVERALL_INVESTIGATION_HS_S"),
            Map.entry("OVERALL_GENERAL_INVESTIGATION", "OVERALL_INVESTIGATION_GENERAL"),
            Map.entry("OVERALL_HS_NO_INVESTIGATION", "OVERALL_NO_INVESTIGATION_HS_S"),
            Map.entry("OVERALL_SENSITIVE_NO_INVESTIGATION", "OVERALL_NO_INVESTIGATION_HS_S"),
            Map.entry("OVERALL_GENERAL_NO_INVESTIGATION", "OVERALL_NO_INVESTIGATION_GENERAL"),
            Map.entry("ESCALATION_CM_MANAGER", "ESCALATION_L1"),
            Map.entry("ESCALATION_DEPARTMENT_DIRECTOR", "ESCALATION_L2"));

    @PostConstruct
    @Transactional
    public void initDefaultSlaConfigsAndHolidays() {
        if (slaConfigRepository.count() == 0) {
            seedDefaultSlaConfigs();
        } else {
            alignConfigsToMatrixKeys();
        }
        if (holidayCalendarRepository.count() == 0) {
            seedDefaultHolidays();
        }
    }

    /**
     * Ensures every matrix key the SLA engine reads exists, carrying over minutes
     * from the equivalent legacy key so approved durations are never changed.
     */
    private void alignConfigsToMatrixKeys() {
        List<SlaConfig> existing = slaConfigRepository.findAll();
        Map<String, SlaConfig> byKey = new HashMap<>();
        existing.forEach(c -> byKey.put(c.getConfigKey(), c));

        List<SlaConfig> canonical = buildDefaultSlaConfigs();
        List<SlaConfig> toInsert = new ArrayList<>();
        for (SlaConfig target : canonical) {
            if (byKey.containsKey(target.getConfigKey())) {
                continue;
            }
            LEGACY_KEY_MIGRATIONS.entrySet().stream()
                    .filter(e -> e.getValue().equals(target.getConfigKey()))
                    .map(e -> byKey.get(e.getKey()))
                    .filter(java.util.Objects::nonNull)
                    .findFirst()
                    .ifPresent(legacy -> {
                        target.setAllowedMinutes(legacy.getAllowedMinutes());
                        log.info("Migrated SLA minutes from legacy key {} to {}", legacy.getConfigKey(),
                                target.getConfigKey());
                    });
            toInsert.add(target);
        }
        if (!toInsert.isEmpty()) {
            slaConfigRepository.saveAll(toInsert);
        }

        List<SlaConfig> stale = existing.stream()
                .filter(c -> LEGACY_KEY_MIGRATIONS.containsKey(c.getConfigKey()))
                .toList();
        if (!stale.isEmpty()) {
            slaConfigRepository.deleteAll(stale);
            log.info("Removed {} legacy SLA config rows superseded by canonical matrix keys", stale.size());
        }
    }

    private void seedDefaultSlaConfigs() {
        slaConfigRepository.saveAll(buildDefaultSlaConfigs());
    }

    private List<SlaConfig> buildDefaultSlaConfigs() {
        return List.of(
            SlaConfig.builder().configKey("BRANCH_COMPLAINT_INTAKE").configGroup(GROUP_INTAKE_SLA).displayName("Branch Office Intake & Registration").allowedMinutes(30).priority(PRIORITY_ALL).description("Timeframe allocated for Branch Manager / CSM complaint intake and initial system logging").build(),
            SlaConfig.builder().configKey("HO_CM_INTAKE").configGroup(GROUP_INTAKE_SLA).displayName("Head Office Customer Management Triage").allowedMinutes(30).priority(PRIORITY_ALL).description("Maximum duration allowed for Head Office CM Division triage and case classification").build(),
            SlaConfig.builder().configKey("CONTACT_CENTER_INTAKE").configGroup(GROUP_INTAKE_SLA).displayName("Omnichannel Contact Center Intake").allowedMinutes(5).priority(PRIORITY_ALL).description("Rapid intake timeframe for Contact Center agents registering live customer inquiries").build(),
            SlaConfig.builder().configKey("SUGGESTION_BOX_INTAKE").configGroup(GROUP_INTAKE_SLA).displayName("Physical Suggestion Box Collection").allowedMinutes(240).priority(PRIORITY_ALL).description("Timeframe allowed for clearing physical branch suggestion boxes and registering entries").build(),
            SlaConfig.builder().configKey("CMD_SCREENING").configGroup(GROUP_STAGE_SLA).displayName("CMD Initial Screening & Acknowledgment").allowedMinutes(240).priority(PRIORITY_ALL).description("Initial screening, case verification, and customer acknowledgment notification").build(),
            SlaConfig.builder().configKey("CMD_FORWARDING").configGroup(GROUP_STAGE_SLA).displayName("Work Unit Assignment & Forwarding").allowedMinutes(240).priority(PRIORITY_ALL).description("Timeframe permitted for transferring classified complaints to target resolution units").build(),
            SlaConfig.builder().configKey("SERVICE_QUALITY_REVIEW").configGroup(GROUP_STAGE_SLA).displayName("Service Quality Governance Audit").allowedMinutes(180).priority(PRIORITY_ALL).description("Governance review by Service Quality officers for sensitive case resolutions").build(),
            SlaConfig.builder().configKey("CXO_REVIEW").configGroup(GROUP_STAGE_SLA).displayName("Chief Experience Officer Review").allowedMinutes(180).priority(PRIORITY_ALL).description("Executive review window for high-value or systemic complaint directions").build(),
            SlaConfig.builder().configKey("CEO_DIRECTION").configGroup(GROUP_STAGE_SLA).displayName("Executive Leadership Directive").allowedMinutes(1440).priority(PRIORITY_ALL).description("Strategic directive timeframe for executive-level case resolutions").build(),
            SlaConfig.builder().configKey("RESOLUTION_NO_INVESTIGATION_HS_S").configGroup(GROUP_STAGE_SLA).displayName("Direct Resolution: Highly Sensitive / Sensitive").allowedMinutes(1920).priority(PRIORITY_HIGHLY_SENSITIVE).description("Resolution timeframe for high-impact complaints handled directly by work units without investigation").build(),
            SlaConfig.builder().configKey("RESOLUTION_NO_INVESTIGATION_GENERAL").configGroup(GROUP_STAGE_SLA).displayName("Direct Resolution: General Category").allowedMinutes(2880).priority(PRIORITY_GENERAL).description("Standard resolution SLA for general banking inquiries handled directly by work units").build(),
            SlaConfig.builder().configKey("INVESTIGATION_CUSTOMER_ACCOUNT").configGroup(GROUP_STAGE_SLA).displayName("Investigation: Customer Accounts & Ledger").allowedMinutes(5280).priority(PRIORITY_ALL).description("Formal investigation period for account reconciliation, ledger, and transaction disputes").build(),
            SlaConfig.builder().configKey("INVESTIGATION_LOAN").configGroup(GROUP_STAGE_SLA).displayName("Investigation: Credit & Facilities").allowedMinutes(7680).priority(PRIORITY_ALL).description("Detailed audit window for loan processing, collateral, and credit facility disputes").build(),
            SlaConfig.builder().configKey("INVESTIGATION_IBD").configGroup(GROUP_STAGE_SLA).displayName("Investigation: International Banking Division").allowedMinutes(7680).priority(PRIORITY_ALL).description("Audit window for foreign trade, remittance, and swift payment investigations").build(),
            SlaConfig.builder().configKey("INVESTIGATION_DIGITAL_BANKING").configGroup(GROUP_STAGE_SLA).displayName("Investigation: Digital Channels & Payment Systems").allowedMinutes(7680).priority(PRIORITY_ALL).description("Technical investigation period for mobile banking, ATM, POS, and electronic payment issues").build(),
            SlaConfig.builder().configKey("COMMITTEE_REVIEW_HS_S").configGroup(GROUP_STAGE_SLA).displayName("Standing Committee Review (Sensitive / HS)").allowedMinutes(1440).priority(PRIORITY_HIGHLY_SENSITIVE).description("Standing Complaint Committee review window for sensitive and high-risk cases").build(),
            SlaConfig.builder().configKey("COMMITTEE_REVIEW_GENERAL").configGroup(GROUP_STAGE_SLA).displayName("Standing Committee Review (General Category)").allowedMinutes(2400).priority(PRIORITY_GENERAL).description("Standing Complaint Committee review window for general category appeals").build(),
            SlaConfig.builder().configKey("CUSTOMER_NOTIFICATION").configGroup(GROUP_CUSTOMER_NOTIFICATION).displayName("Customer Final Resolution Dispatch").allowedMinutes(240).priority(PRIORITY_ALL).description("Timeframe allowed for dispatching final written resolution notice to customer via SMS & Email").build(),
            SlaConfig.builder().configKey("OVERALL_INVESTIGATION_HS_S").configGroup(GROUP_OVERALL_SLA).displayName("Total Lifecycle: Highly Sensitive / Sensitive (With Inv)").allowedMinutes(13440).priority(PRIORITY_HIGHLY_SENSITIVE).description("Maximum end-to-end complaint lifecycle including formal investigation").build(),
            SlaConfig.builder().configKey("OVERALL_INVESTIGATION_GENERAL").configGroup(GROUP_OVERALL_SLA).displayName("Total Lifecycle: General (With Inv)").allowedMinutes(14400).priority(PRIORITY_GENERAL).description("Maximum total complaint lifecycle for general category cases requiring formal investigation").build(),
            SlaConfig.builder().configKey("OVERALL_NO_INVESTIGATION_HS_S").configGroup(GROUP_OVERALL_SLA).displayName("Total Lifecycle: Highly Sensitive / Sensitive (No Inv)").allowedMinutes(3840).priority(PRIORITY_HIGHLY_SENSITIVE).description("Total allowable lifecycle without formal investigation").build(),
            SlaConfig.builder().configKey("OVERALL_NO_INVESTIGATION_GENERAL").configGroup(GROUP_OVERALL_SLA).displayName("Total Lifecycle: General (No Inv)").allowedMinutes(4800).priority(PRIORITY_GENERAL).description("Total allowable lifecycle for general category complaints resolved without investigation").build(),
            SlaConfig.builder().configKey("ESCALATION_L1").configGroup(GROUP_ESCALATION_SLA).displayName("Management Escalation: CM Division Head").allowedMinutes(720).priority(PRIORITY_ALL).description("Allocated timeframe for CM Division Manager oversight intervention following an SLA breach").build(),
            SlaConfig.builder().configKey("ESCALATION_L2").configGroup(GROUP_ESCALATION_SLA).displayName("Executive Escalation: Department Director").allowedMinutes(960).priority(PRIORITY_ALL).description("Timeframe permitted for Department Director / Regional Director intervention upon tier-2 escalation").build()
        );
    }

    private void seedDefaultHolidays() {
        int year = LocalDate.now(ZoneId.systemDefault()).getYear();
        List<HolidayCalendar> defaultHolidays = List.of(
            HolidayCalendar.builder().holidayDate(LocalDate.of(year, Month.JANUARY, 7)).holidayName("Ethiopian Genna / Christmas").holidayType(TYPE_PUBLIC_HOLIDAY).build(),
            HolidayCalendar.builder().holidayDate(LocalDate.of(year, Month.JANUARY, 19)).holidayName("Ethiopian Epiphany / Timket").holidayType(TYPE_PUBLIC_HOLIDAY).build(),
            HolidayCalendar.builder().holidayDate(LocalDate.of(year, Month.MARCH, 2)).holidayName("Victory of Adwa").holidayType(TYPE_PUBLIC_HOLIDAY).build(),
            HolidayCalendar.builder().holidayDate(LocalDate.of(year, Month.MAY, 1)).holidayName("International Labour Day").holidayType(TYPE_PUBLIC_HOLIDAY).build(),
            HolidayCalendar.builder().holidayDate(LocalDate.of(year, Month.MAY, 5)).holidayName("Patriots Victory Day").holidayType(TYPE_PUBLIC_HOLIDAY).build(),
            HolidayCalendar.builder().holidayDate(LocalDate.of(year, Month.SEPTEMBER, 11)).holidayName("Ethiopian New Year (Enkutatash)").holidayType(TYPE_PUBLIC_HOLIDAY).build(),
            HolidayCalendar.builder().holidayDate(LocalDate.of(year, Month.SEPTEMBER, 27)).holidayName("Finding of True Cross (Meskel)").holidayType(TYPE_PUBLIC_HOLIDAY).build()
        );
        holidayCalendarRepository.saveAll(defaultHolidays);
    }

    public List<SlaConfig> getAllConfigs() {
        return slaConfigRepository.findAll();
    }

    @Transactional
    public void resetSlaConfigs() {
        slaConfigRepository.deleteAll();
        seedDefaultSlaConfigs();
    }

    /**
     * Reads an approved SLA Matrix duration. Missing keys are logged and return
     * empty so callers cannot silently invent minutes.
     */
    public Optional<Integer> resolveAllowedMinutes(String configKey) {
        if (configKey == null || configKey.isBlank()) {
            log.error("SLA Matrix lookup skipped: config key is blank");
            return Optional.empty();
        }
        Optional<SlaConfig> configOpt = slaConfigRepository.findByConfigKey(configKey);
        if (configOpt.isEmpty() || configOpt.get().getAllowedMinutes() == null) {
            log.error("SLA Matrix key missing or has no allowed minutes: {}", configKey);
            return Optional.empty();
        }
        return Optional.of(configOpt.get().getAllowedMinutes());
    }

    @Transactional
    public SlaConfig updateConfig(Long id, Integer allowedMinutes, String displayName, String description) {
        SlaConfig config = slaConfigRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("SLA Config not found with ID: " + id));
        if (allowedMinutes != null) config.setAllowedMinutes(allowedMinutes);
        if (displayName != null) config.setDisplayName(displayName);
        if (description != null) config.setDescription(description);
        return slaConfigRepository.save(config);
    }

    @Transactional
    public List<HolidayCalendar> getAllHolidays() {
        return holidayCalendarRepository.findAll();
    }

    @Transactional
    public HolidayCalendar addHoliday(LocalDate date, String name, String type) {
        HolidayCalendar h = HolidayCalendar.builder()
                .holidayDate(date)
                .holidayName(name)
                .holidayType(type != null ? type : TYPE_PUBLIC_HOLIDAY)
                .build();
        return holidayCalendarRepository.save(h);
    }

    @Transactional
    public void purgeAllSlaData() {
        if (slaMetricsRepository != null) {
            slaMetricsRepository.deleteAll();
        }
        if (auditLogRepository != null) {
            auditLogRepository.deleteAll();
        }
    }

    @Transactional
    public void deleteHoliday(Long id) {
        holidayCalendarRepository.deleteById(id);
    }
}
