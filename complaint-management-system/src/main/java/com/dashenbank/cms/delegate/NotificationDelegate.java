package com.dashenbank.cms.delegate;

import com.dashenbank.cms.repository.ComplaintSlaMetricsRepository;
import com.dashenbank.cms.model.CustomerFeedback;
import com.dashenbank.cms.repository.CustomerFeedbackRepository;
import com.dashenbank.cms.service.AuditService;
import com.dashenbank.cms.service.NotificationService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;
import org.flowable.variable.api.delegate.VariableScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component("notificationDelegate")
public class NotificationDelegate implements JavaDelegate, TaskListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationDelegate.class);
    private static final String LANG_AMHARIC = "amharic";
    private static final String VAR_CASE_HISTORY = "caseHistory";
    private static final String VAR_STATUS = "status";
    private static final String VAR_DECISION = "decision";
    private static final String VAR_PREFERRED_LANGUAGE = "preferredLanguage";
    private static final String STATUS_DECLINED = "DECLINED";
    private static final String STATUS_CLOSED = "CLOSED";
    private static final String ACTOR_SYSTEM = "system";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();

    private final NotificationService notificationService;
    private final AuditService auditService;
    private final CustomerFeedbackRepository feedbackRepository;
    private final ComplaintSlaMetricsRepository slaMetricsRepository;

    @Autowired
    public NotificationDelegate(
            NotificationService notificationService,
            AuditService auditService,
            CustomerFeedbackRepository feedbackRepository,
            ComplaintSlaMetricsRepository slaMetricsRepository) {
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.feedbackRepository = feedbackRepository;
        this.slaMetricsRepository = slaMetricsRepository;
    }

    @Override
    public void notify(DelegateTask delegateTask) {
        executeLogic(delegateTask, (String) delegateTask.getVariable("customNotificationMessage"));
    }

    @Override
    public void execute(DelegateExecution execution) {
        executeLogic(execution, (String) execution.getVariable("customNotificationMessage"));
    }

    public void sendNotificationFromMap(Map<String, Object> processVars, Map<String, Object> customerMap,
            String ticketId, String processInstanceId) {
        if (processVars == null)
            processVars = new HashMap<>();
        if (customerMap == null)
            customerMap = new HashMap<>();

        // Per requirement: Only send Resolution Notification when complaint reaches a
        // final resolved/closed state.
        boolean isDeclined = isDeclinedStatus(
                (String) processVars.get(VAR_STATUS),
                (String) processVars.get(VAR_DECISION),
                (String) processVars.get("classification"));
        if (isDeclined) {
            log.info("Complaint {} is DECLINED. Skipping customer notification.", ticketId);
            return;
        }

        String status = (String) processVars.get(VAR_STATUS);
        String currentStage = (String) processVars.get("currentStage");
        String stage = (String) processVars.get("stage");
        boolean isCcoFinalClosure = isCcoFinalClosure(
                (Boolean) processVars.get("resolutionAccepted"), status, currentStage, stage);

        if (!isCcoFinalClosure) {
            log.info("Complaint {} is in stage status={}, decision={}. Skipping resolution notification until CCO closure.",
                    ticketId, status, processVars.get(VAR_DECISION));
            return;
        }

        String email = (String) customerMap.get("email");
        String phone = (String) customerMap.get("phone");
        String customerName = (String) customerMap.getOrDefault("name", "Valued Customer");
        String preferredLanguage = (String) processVars.getOrDefault(VAR_PREFERRED_LANGUAGE,
                customerMap.getOrDefault(VAR_PREFERRED_LANGUAGE, "english"));

        String token = createFeedbackRecord(ticketId, processInstanceId, preferredLanguage);
        String feedbackLink = "http://localhost:3000/customer-feedback?token=" + token;

        String subDate = extractSubmissionDate(processVars);
        String resDate = LocalDateTime.now(SYSTEM_ZONE).format(DATE_FMT);
        String resSummary = extractResolutionSummary(processVars, null);

        String[] composed = composeResolutionMessage(
                preferredLanguage, customerName, ticketId, subDate, resDate, resSummary, feedbackLink);
        String subject = composed[0];
        String message = composed[1];

        if (email != null && !email.isBlank()) {
            notificationService.sendEmail(email, subject, message);
        }
        if (phone != null && !phone.isBlank()) {
            notificationService.sendSms(phone, message);
        }
        auditService.log(ticketId, processInstanceId, null, "NOTIFICATION_SENT", ACTOR_SYSTEM, ACTOR_SYSTEM,
                "Resolution notification sent to customer: " + subject);
    }

    @SuppressWarnings("unchecked")
    private void executeLogic(VariableScope execution, String customMessage) {
        Boolean resNotifSent = (Boolean) execution.getVariable("resolution.notification.sent");
        if (Boolean.TRUE.equals(resNotifSent)) {
            return;
        }

        boolean isDeclined = isDeclinedStatus(
                (String) execution.getVariable(VAR_STATUS),
                (String) execution.getVariable(VAR_DECISION),
                (String) execution.getVariable("classification"));
        if (isDeclined) {
            log.info("Execution is DECLINED. Skipping resolution notification.");
            return;
        }

        boolean isCcoFinalClosure = isCcoFinalClosure(
                (Boolean) execution.getVariable("resolutionAccepted"),
                (String) execution.getVariable(VAR_STATUS),
                (String) execution.getVariable("currentStage"),
                (String) execution.getVariable("stage"));
        if (!isCcoFinalClosure) {
            log.info("Skipping BPMN resolution notification until CCO closure. status={}, decision={}",
                    execution.getVariable(VAR_STATUS), execution.getVariable(VAR_DECISION));
            return;
        }

        Object rawComplaint = execution.getVariable("complaint");
        Object rawCustomer = execution.getVariable("customer");
        Map<String, Object> complaint = rawComplaint instanceof Map<?, ?> map ? (Map<String, Object>) map : null;
        Map<String, Object> customer = rawCustomer instanceof Map<?, ?> map ? (Map<String, Object>) map : null;

        String ticketId = resolveTicketId(execution, complaint);
        String processInstanceId = extractProcessInstanceId(execution);

        String email = customer != null ? (String) customer.get("email") : null;
        String phone = customer != null ? (String) customer.get("phone") : null;
        String customerName = customer != null ? (String) customer.get("name") : "Valued Customer";
        String preferredLanguage = resolvePreferredLanguage(execution, customer);

        String token = createFeedbackRecord(ticketId, processInstanceId, preferredLanguage);
        String feedbackLink = "http://localhost:3000/customer-feedback?token=" + token;

        Map<String, Object> vars = execution.getVariables();
        String subDate = extractSubmissionDate(vars);
        String resDate = LocalDateTime.now(SYSTEM_ZONE).format(DATE_FMT);
        String resSummary = extractResolutionSummary(vars, customMessage);

        String[] composed = composeResolutionMessage(
                preferredLanguage, customerName, ticketId, subDate, resDate, resSummary, feedbackLink);
        String subject = composed[0];
        String message = composed[1];

        if (email != null && !email.isBlank()) {
            notificationService.sendEmail(email, subject, message);
            execution.setVariable("notification.emailSent", true);
        }
        if (phone != null && !phone.isBlank()) {
            notificationService.sendSms(phone, message);
            execution.setVariable("notification.smsSent", true);
        }
        execution.setVariable("resolution.notification.sent", true);
        execution.setVariable("notification.sentAt", LocalDateTime.now(SYSTEM_ZONE).toString());

        appendHistory(execution, "Resolution notification sent for ticket=" + ticketId);
        auditService.log(ticketId, processInstanceId, null, "NOTIFICATION_SENT", ACTOR_SYSTEM, ACTOR_SYSTEM,
                "Resolution notification sent to customer.");
    }

    private boolean isDeclinedStatus(String status, String decision, String classification) {
        return STATUS_DECLINED.equalsIgnoreCase(status)
                || STATUS_DECLINED.equalsIgnoreCase(decision)
                || STATUS_DECLINED.equalsIgnoreCase(classification);
    }

    private boolean isCcoFinalClosure(Boolean resolutionAccepted, String status, String currentStage, String stage) {
        return Boolean.TRUE.equals(resolutionAccepted)
                || STATUS_CLOSED.equalsIgnoreCase(status)
                || STATUS_CLOSED.equalsIgnoreCase(currentStage)
                || STATUS_CLOSED.equalsIgnoreCase(stage);
    }

    private String resolveTicketId(VariableScope execution, Map<String, Object> complaint) {
        String dbcTicketId = (String) execution.getVariable("dbcTicketId");
        if (dbcTicketId == null || !dbcTicketId.startsWith("DBC-")) {
            dbcTicketId = (String) execution.getVariable("complaintId");
        }
        if (dbcTicketId != null && dbcTicketId.startsWith("DBC-")) {
            return dbcTicketId;
        }
        if (complaint != null && complaint.get("id") != null) {
            return (String) complaint.get("id");
        }
        return "unknown";
    }

    private String resolvePreferredLanguage(VariableScope execution, Map<String, Object> customer) {
        String preferredLanguage = (String) execution.getVariable(VAR_PREFERRED_LANGUAGE);
        if (preferredLanguage == null && customer != null) {
            preferredLanguage = (String) customer.getOrDefault(VAR_PREFERRED_LANGUAGE, "english");
        }
        return preferredLanguage;
    }

    private String[] composeResolutionMessage(String preferredLanguage, String customerName, String ticketId,
            String subDate, String resDate, String resSummary, String feedbackLink) {
        if (LANG_AMHARIC.equalsIgnoreCase(preferredLanguage)) {
            return new String[] {
                    "የቅሬታ መፍትሄ መረጃ",
                    String.format(
                            "ውድ %s፣%n%n" +
                                    "በ %s ያቀረቡት ቅሬታ (%s) መፍትሄ አግኝቷል።%n%n" +
                                    "የመፍትሄ ማጠቃለያ:%n%s%n%n" +
                                    "የተፈታበት ቀን:%n%s%n%n" +
                                    "የእርስዎ ተሞክሮ ለእኛ አስፈላጊ ነው።%n%n" +
                                    "እባክዎን ከታች ያለውን ሊንክ በመጠቀም በአገልግሎታችን ላይ ያለዎትን እርካታ ይመዝኑ:%n%n" +
                                    "%s%n%n" +
                                    "አገልግሎታችንን እንድናሻሽል ስለረዱን እናመሰግናለን።%n%n" +
                                    "ዳሽን ባንክ%n" +
                                    "የደንበኞች አገልግሎት ቡድን",
                            customerName, subDate, ticketId, resSummary, resDate, feedbackLink)
            };
        }
        return new String[] {
                "Complaint Resolution Update",
                String.format(
                        "Dear %s,%n%n" +
                                "Your complaint (%s) submitted on %s has been resolved.%n%n" +
                                "Resolution:%n%s%n%n" +
                                "Resolution Date:%n%s%n%n" +
                                "Your experience matters to us.%n%n" +
                                "Please take a moment to rate your satisfaction with our service using the link below:%n%n"
                                +
                                "%s%n%n" +
                                "Thank you for helping us improve our services.%n%n" +
                                "Dashen Bank%n" +
                                "Customer Care Team",
                        customerName, ticketId, subDate, resSummary, resDate, feedbackLink)
        };
    }

    private String extractSubmissionDate(Map<String, Object> vars) {
        if (vars == null)
            return LocalDateTime.now(SYSTEM_ZONE).format(DATE_FMT);
        String subDate = (String) vars.get("submissionDate");
        if (subDate == null || subDate.isBlank()) {
            subDate = (String) vars.get("createdAt");
        }
        if (subDate == null || subDate.isBlank()) {
            subDate = (String) vars.get("dateOfComplaint");
        }
        if (subDate != null && subDate.length() >= 10) {
            return subDate.substring(0, 10);
        }
        return LocalDateTime.now(SYSTEM_ZONE).format(DATE_FMT);
    }

    private String extractResolutionSummary(Map<String, Object> vars, String customMessage) {
        if (vars == null)
            vars = Map.of();
        String summary = (String) vars.get("resolutionSummary");
        if (summary == null || summary.isBlank()) {
            summary = (String) vars.get("notes");
        }
        if (summary == null || summary.isBlank()) {
            summary = (String) vars.get("resolutionDetails");
        }
        if (summary == null || summary.isBlank()) {
            summary = (String) vars.get("additionalRemarks");
        }
        if (summary == null || summary.isBlank()) {
            summary = (String) vars.get("committeeExplanation");
        }
        if (summary == null || summary.isBlank()) {
            summary = (String) vars.get("decisionSummary");
        }
        if (summary == null || summary.isBlank()) {
            summary = customMessage;
        }
        if (summary == null || summary.isBlank()) {
            summary = "Complaint reviewed, resolution verified, and case closed by Customer Care Officer.";
        }
        return summary;
    }

    private String createFeedbackRecord(String ticketId, String processInstanceId, String preferredLanguage) {
        String token = UUID.randomUUID().toString();
        int currentReopenCount = 0;

        String formalTicketId = ticketId;
        if (formalTicketId != null && formalTicketId.startsWith("CM-") && processInstanceId != null) {
            try {
                var metricsOpt = slaMetricsRepository.findByProcessInstanceId(processInstanceId);
                if (metricsOpt.isPresent() && metricsOpt.get().getComplaintId() != null
                        && metricsOpt.get().getComplaintId().startsWith("DBC-")) {
                    formalTicketId = metricsOpt.get().getComplaintId();
                }
            } catch (Exception e) {
                log.debug("Could not resolve DBC ticket from SLA metrics: {}", e.getMessage());
            }
        }

        try {
            List<CustomerFeedback> existing = feedbackRepository.findAllByTicketNumber(formalTicketId);
            if (existing != null) {
                currentReopenCount = existing.size();
            }
        } catch (Exception e) {
            log.error("Failed to read existing feedback records: {}", e.getMessage());
        }

        try {
            CustomerFeedback fb = CustomerFeedback.builder()
                    .ticketNumber(formalTicketId)
                    .complaintId(processInstanceId)
                    .secureToken(token)
                    .tokenExpired(false)
                    .feedbackRequestSentAt(LocalDateTime.now(SYSTEM_ZONE))
                    .reopenCount(currentReopenCount)
                    .reopenedCase(false)
                    .preferredLanguage(preferredLanguage)
                    .build();
            feedbackRepository.save(fb);
        } catch (Exception e) {
            log.error("Failed to pre-create CustomerFeedback entry: {}", e.getMessage());
        }
        return token;
    }

    private String extractProcessInstanceId(VariableScope execution) {
        if (execution instanceof DelegateExecution delegateExecution) {
            return delegateExecution.getProcessInstanceId();
        } else if (execution instanceof DelegateTask delegateTask) {
            return delegateTask.getProcessInstanceId();
        }
        return null;
    }

    private void appendHistory(VariableScope execution, String event) {
        Object historyVar = execution.getVariable(VAR_CASE_HISTORY);
        if (historyVar == null) {
            execution.setVariable(VAR_CASE_HISTORY, event);
        } else {
            execution.setVariable(VAR_CASE_HISTORY, historyVar.toString() + System.lineSeparator() + event);
        }
    }
}
