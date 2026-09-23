package com.dashenbank.cms.delegate;

import com.dashenbank.cms.customer.CustomerContactPhones;
import com.dashenbank.cms.notification.CustomerNotifications;
import com.dashenbank.cms.notification.NotificationQueueResult;
import com.dashenbank.cms.notification.NotificationService;
import com.dashenbank.cms.service.AuditService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

@Component("generateTicketDelegate")
public class GenerateTicketDelegate implements JavaDelegate {

    private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();
    private static final String KEY_CASE_HISTORY = "caseHistory";

    private final NotificationService notificationService;
    private final AuditService auditService;
    private final com.dashenbank.cms.service.SlaTrackingService slaTrackingService;

    public GenerateTicketDelegate(NotificationService notificationService, AuditService auditService,
            com.dashenbank.cms.service.SlaTrackingService slaTrackingService) {
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.slaTrackingService = slaTrackingService;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castToMap(Object obj) {
        if (obj instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    @Override
    public void execute(DelegateExecution execution) {
        Map<String, Object> complaint = new HashMap<>(castToMap(execution.getVariable("complaint")));

        String ticket = resolveTicket(execution);

        complaint.put("id", ticket);
        execution.setVariable("complaint", complaint);
        execution.setVariable("dbcTicketId", ticket);
        execution.setVariable("createdAt", LocalDateTime.now(SYSTEM_ZONE).toString());

        applySlaDefaults(execution);
        appendHistory(execution, "Ticket generated: " + ticket);
        sendRegistrationConfirmationIfNeeded(execution, ticket);

        auditService.log(ticket, execution.getProcessInstanceId(), null, "TICKET_GENERATED", "system", "system",
                "Ticket " + ticket + " has been successfully generated.");
    }

    private String resolveTicket(DelegateExecution execution) {
        Object dbcVar = execution.getVariable("dbcTicketId");
        Object cIdVar = execution.getVariable("complaintId");
        if (dbcVar != null && dbcVar.toString().startsWith("DBC-")) {
            return dbcVar.toString();
        }
        if (cIdVar != null && cIdVar.toString().startsWith("DBC-")) {
            return cIdVar.toString();
        }
        return slaTrackingService.generateDbcTicketId();
    }

    private void applySlaDefaults(DelegateExecution execution) {
        Map<String, Object> sla = new HashMap<>(castToMap(execution.getVariable("sla")));
        sla.put("deadline", LocalDateTime.now(SYSTEM_ZONE).plusHours(24).toString());
        sla.put("breached", false);
        sla.put("reminderCount", 0);
        execution.setVariable("sla", sla);
    }

    private void sendRegistrationConfirmationIfNeeded(DelegateExecution execution, String ticket) {
        // Per requirement: Only ONE Complaint Registration Confirmation notification
        // per complaint lifecycle
        if (Boolean.TRUE.equals(execution.getVariable("ack.sent"))) {
            return;
        }

        Map<String, Object> customer = castToMap(execution.getVariable("customer"));
        NotificationQueueResult queued = notificationService.notifyCustomer(CustomerNotifications.registration(
                ticket,
                execution.getProcessInstanceId(),
                (String) customer.getOrDefault("name", "Valued Customer"),
                (String) customer.get("email"),
                CustomerContactPhones.currentContact(customer),
                resolvePreferredLanguage(execution, customer),
                LocalDateTime.now(SYSTEM_ZONE)));

        execution.setVariable("notification.ticketEmailQueued", queued.emailQueued());
        execution.setVariable("notification.ticketSmsQueued", queued.smsQueued());
        execution.setVariable("ack.sent", true);
    }

    private String resolvePreferredLanguage(DelegateExecution execution, Map<String, Object> customer) {
        String preferredLanguage = (String) execution.getVariable("preferredLanguage");
        if (preferredLanguage == null && !customer.isEmpty()) {
            preferredLanguage = (String) customer.getOrDefault("preferredLanguage", "english");
        }
        return preferredLanguage;
    }

    private void appendHistory(DelegateExecution execution, String event) {
        Object historyVar = execution.getVariable(KEY_CASE_HISTORY);
        if (historyVar == null) {
            execution.setVariable(KEY_CASE_HISTORY, event);
        } else {
            execution.setVariable(KEY_CASE_HISTORY, historyVar.toString() + "\n" + event);
        }
    }
}
