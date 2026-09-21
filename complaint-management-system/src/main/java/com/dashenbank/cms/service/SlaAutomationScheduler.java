package com.dashenbank.cms.service;

import com.dashenbank.cms.model.ComplaintSlaMetrics;
import com.dashenbank.cms.model.SlaBreachRecord;
import com.dashenbank.cms.repository.ComplaintSlaMetricsRepository;
import com.dashenbank.cms.repository.SlaBreachRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Component
public class SlaAutomationScheduler {

        private static final Logger log = LoggerFactory.getLogger(SlaAutomationScheduler.class);
        private static final String SYSTEM_ACTOR = "SYSTEM";

        private final ComplaintSlaMetricsRepository slaMetricsRepository;
        private final SlaTrackingService slaTrackingService;
        private final AuditService auditService;
        private final SlaBreachRecordRepository breachRecordRepository;

        @Autowired
        public SlaAutomationScheduler(
                        ComplaintSlaMetricsRepository slaMetricsRepository,
                        SlaTrackingService slaTrackingService,
                        AuditService auditService,
                        SlaBreachRecordRepository breachRecordRepository) {
                this.slaMetricsRepository = slaMetricsRepository;
                this.slaTrackingService = slaTrackingService;
                this.auditService = auditService;
                this.breachRecordRepository = breachRecordRepository;
        }

        /**
         * Periodically checks all active complaints every 60 seconds to evaluate
         * business hours SLAs, issue the 80% reminder, and record 100% breaches.
         */
        @Scheduled(fixedDelay = 60000)
        @Transactional
        public void runSlaMonitoring() {
                List<ComplaintSlaMetrics> activeCases = slaMetricsRepository.findAll().stream()
                                .filter(m -> !"CLOSED".equalsIgnoreCase(m.getStatus())
                                                && !"COMPLETED".equalsIgnoreCase(m.getCurrentStage())
                                                && !SlaTrackingService.isOtherClassification(m))
                                .toList();

                if (activeCases.isEmpty()) {
                        return;
                }

                LocalDateTime now = LocalDateTime.now(ZoneId.of("Africa/Addis_Ababa"));

                for (ComplaintSlaMetrics m : activeCases) {
                        try {
                                processComplaintSla(m, now);
                        } catch (Exception e) {
                                log.error("Error processing SLA automation for instance {}", m.getProcessInstanceId(),
                                                e);
                        }
                }
        }

        private void processComplaintSla(ComplaintSlaMetrics m, LocalDateTime now) {
                slaTrackingService.recalculateSlaStatus(m);
                Integer allowedObj = m.getTotalAllowedMinutes();
                if (allowedObj == null) {
                    log.error("Skipping SLA scheduler for {}: totalAllowedMinutes is null", m.getProcessInstanceId());
                    return;
                }
                int allowed = allowedObj;
                int elapsed = m.getTotalElapsedMinutes() != null ? m.getTotalElapsedMinutes() : 0;
                double consumptionRatio = (double) elapsed / (double) Math.max(1, allowed);

                String complaintId = m.getComplaintId() != null ? m.getComplaintId()
                                : "DBC-" + m.getProcessInstanceId();

                checkApproachingReminder(m, consumptionRatio, complaintId);
                checkBreachAlert(m, consumptionRatio, elapsed, allowed, complaintId, now);

                slaMetricsRepository.save(m);
        }

        private void checkApproachingReminder(ComplaintSlaMetrics m, double consumptionRatio, String complaintId) {
                if (consumptionRatio >= 0.80 && consumptionRatio < 1.0
                                && (m.getReminder1Sent() == null || !m.getReminder1Sent())) {
                        m.setReminder1Sent(true);
                        m.setSlaStatus("APPROACHING");
                        String responsibleUnit = m.getDepartment() != null && !m.getDepartment().isBlank()
                                        ? m.getDepartment()
                                        : (m.getBranch() != null && !m.getBranch().isBlank() ? m.getBranch()
                                                        : "Customer Care / Work Unit");
                        String stage = m.getCurrentStage() != null ? m.getCurrentStage() : "Workflow Processing";
                        String deadline = m.getExpectedResolutionDate() != null
                                        ? m.getExpectedResolutionDate().toString()
                                        : "Standard SLA";
                        String msg = String.format(
                                        "SLA WARNING [Ticket: %s | Status: Warning | Stage: %s | Unit: %s | Deadline: %s | Time: %s]",
                                        complaintId, stage, responsibleUnit, deadline,
                                        LocalDateTime.now(ZoneId.systemDefault()));

                        auditService.log(complaintId, m.getProcessInstanceId(), null, "SLA_REMINDER_SENT", SYSTEM_ACTOR,
                                        responsibleUnit, msg, m.getCustomerName(), m.getComplaintCategory(), null);
                        log.info(">>> Triggered 80% SLA reminder for Ticket {} (Unit: {})", complaintId,
                                        responsibleUnit);
                }
        }

        private void checkBreachAlert(ComplaintSlaMetrics m, double consumptionRatio, int elapsed, int allowed,
                        String complaintId, LocalDateTime now) {
                if (consumptionRatio >= 1.0 && (m.getReminder2Sent() == null || !m.getReminder2Sent())) {
                        m.setReminder2Sent(true);
                        m.setBreached(true);
                        m.setBreachedAt(now);
                        m.setSlaStatus("BREACHED");
                        String responsibleUnit = m.getDepartment() != null && !m.getDepartment().isBlank()
                                        ? m.getDepartment()
                                        : (m.getBranch() != null && !m.getBranch().isBlank() ? m.getBranch()
                                                        : "Customer Care / Work Unit");
                        String stage = m.getCurrentStage() != null ? m.getCurrentStage() : "Workflow Processing";
                        String deadline = m.getExpectedResolutionDate() != null
                                        ? m.getExpectedResolutionDate().toString()
                                        : "Standard SLA";
                        String msg = String.format(
                                        "SLA BREACHED [Ticket: %s | Status: Breached | Stage: %s | Unit: %s | Deadline: %s | Time: %s]",
                                        complaintId, stage, responsibleUnit, deadline, now);

                        auditService.log(complaintId, m.getProcessInstanceId(), null, "SLA_BREACHED", SYSTEM_ACTOR,
                                        responsibleUnit, msg, m.getCustomerName(), m.getComplaintCategory(), null);

                        SlaBreachRecord breach = SlaBreachRecord.builder()
                                        .complaintId(complaintId)
                                        .processInstanceId(m.getProcessInstanceId())
                                        .stageName(stage)
                                        .breachReason("SLA allowed minutes exceeded (" + elapsed + "/" + allowed
                                                        + " mins)")
                                        .breachDurationMinutes((long) (elapsed - allowed))
                                        .responsibleWorkUnit(responsibleUnit)
                                        .escalationActionsTaken("Automated 100% SLA breach alert dispatched to "
                                                        + responsibleUnit)
                                        .build();
                        breachRecordRepository.save(breach);
                        log.warn(">>> Recorded SLA Breach for Ticket {} (Unit: {})", complaintId, responsibleUnit);
                }
        }
}
