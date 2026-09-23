package com.dashenbank.cms.delegate;

import com.dashenbank.cms.config.AppHttpProperties;
import com.dashenbank.cms.customer.CustomerContactPhones;
import com.dashenbank.cms.repository.ComplaintSlaMetricsRepository;
import com.dashenbank.cms.model.CustomerFeedback;
import com.dashenbank.cms.repository.CustomerFeedbackRepository;
import com.dashenbank.cms.notification.CustomerNotifications;
import com.dashenbank.cms.notification.NotificationEventType;
import com.dashenbank.cms.notification.NotificationQueueResult;
import com.dashenbank.cms.notification.NotificationRequest;
import com.dashenbank.cms.notification.NotificationService;
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
    private static final String VAR_CASE_HISTORY = "caseHistory";
    private static final String VAR_STATUS = "status";
    private static final String VAR_DECISION = "decision";
    private static final String VAR_PREFERRED_LANGUAGE = "preferredLanguage";
    private static final String STATUS_DECLINED = "DECLINED";
    private static final String STATUS_CLOSED = "CLOSED";
    private static final DateTimeFormatter DATE_FMT = CustomerNotifications.DATE;
    private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();

    private final NotificationService notificationService;
    private final CustomerFeedbackRepository feedbackRepository;
    private final ComplaintSlaMetricsRepository slaMetricsRepository;
    private final AppHttpProperties appHttpProperties;

    @Autowired
    public NotificationDelegate(
            NotificationService notificationService,
            CustomerFeedbackRepository feedbackRepository,
            ComplaintSlaMetricsRepository slaMetricsRepository,
            AppHttpProperties appHttpProperties) {
        this.notificationService = notificationService;
        this.feedbackRepository = feedbackRepository;
        this.slaMetricsRepository = slaMetricsRepository;
        this.appHttpProperties = appHttpProperties;
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

        String preferredLanguage = (String) processVars.getOrDefault(VAR_PREFERRED_LANGUAGE,
                customerMap.getOrDefault(VAR_PREFERRED_LANGUAGE, "english"));

        queueResolutionNotice(ticketId, processInstanceId, customerMap, preferredLanguage, processVars, null);
    }

    /**
     * Pre-creates the {@link CustomerFeedback} row that the survey link in a
     * resolution notification points to, and returns its secure token.
     */
    public String issueFeedbackToken(String ticketId, String processInstanceId, String preferredLanguage) {
        return createFeedbackRecord(ticketId, processInstanceId, preferredLanguage);
    }

    public String feedbackLink(String token) {
        return appHttpProperties.pageUrl("/customer-feedback?token=" + token);
    }

    private NotificationQueueResult queueResolutionNotice(String ticketId, String processInstanceId,
            Map<String, Object> customer, String preferredLanguage, Map<String, Object> vars, String customMessage) {
        String token = createFeedbackRecord(ticketId, processInstanceId, preferredLanguage);
        String customerName = customer != null && customer.get("name") != null
                ? customer.get("name").toString()
                : "Valued Customer";
        return notificationService.notifyCustomer(NotificationRequest.builder()
                .eventType(NotificationEventType.COMPLAINT_RESOLVED)
                .complaintRef(ticketId)
                .processInstanceId(processInstanceId)
                .preferredLanguage(preferredLanguage)
                .email(customer != null ? (String) customer.get("email") : null)
                .phone(CustomerContactPhones.currentContact(customer))
                .occurrenceKey(token)
                .variable(CustomerNotifications.CUSTOMER_NAME, customerName)
                .variable(CustomerNotifications.TICKET_ID, ticketId)
                .variable(CustomerNotifications.SUBMISSION_DATE, extractSubmissionDate(vars))
                .variable(CustomerNotifications.RESOLUTION_DATE, LocalDateTime.now(SYSTEM_ZONE).format(DATE_FMT))
                .variable(CustomerNotifications.RESOLUTION_SUMMARY, extractResolutionSummary(vars, customMessage))
                .variable(CustomerNotifications.FEEDBACK_LINK, feedbackLink(token))
                .build());
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
        String preferredLanguage = resolvePreferredLanguage(execution, customer);

        NotificationQueueResult queued = queueResolutionNotice(ticketId, processInstanceId, customer,
                preferredLanguage, execution.getVariables(), customMessage);

        execution.setVariable("notification.resolutionEmailQueued", queued.emailQueued());
        execution.setVariable("notification.resolutionSmsQueued", queued.smsQueued());
        execution.setVariable("resolution.notification.sent", true);
        execution.setVariable("notification.queuedAt", LocalDateTime.now(SYSTEM_ZONE).toString());

        appendHistory(execution, "Resolution notification queued for ticket=" + ticketId);
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
