package com.dashenbank.cms.delegate;

import com.dashenbank.cms.customer.CustomerContactPhones;
import com.dashenbank.cms.notification.CustomerNotifications;
import com.dashenbank.cms.notification.NotificationQueueResult;
import com.dashenbank.cms.notification.NotificationService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@Component("sendAcknowledgmentDelegate")
public class SendAcknowledgmentDelegate implements JavaDelegate {

    private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();
    private static final String VAR_CASE_HISTORY = "caseHistory";
    private final NotificationService notificationService;

    public SendAcknowledgmentDelegate(NotificationService notificationService) {
        this.notificationService = notificationService;
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
        Boolean ackSent = (Boolean) execution.getVariable("ack.sent");
        if (Boolean.TRUE.equals(ackSent)) {
            return;
        }

        Map<String, Object> complaint = castToMap(execution.getVariable("complaint"));
        Map<String, Object> customer = castToMap(execution.getVariable("customer"));

        String ticketId = complaint.containsKey("id") ? (String) complaint.get("id") : "unknown";

        String preferredLanguage = (String) execution.getVariable("preferredLanguage");
        if (preferredLanguage == null && !customer.isEmpty()) {
            preferredLanguage = (String) customer.getOrDefault("preferredLanguage", "english");
        }

        LocalDateTime now = LocalDateTime.now(SYSTEM_ZONE);
        NotificationQueueResult queued = notificationService.notifyCustomer(CustomerNotifications.registration(
                ticketId,
                execution.getProcessInstanceId(),
                (String) customer.getOrDefault("name", "Valued Customer"),
                (String) customer.get("email"),
                CustomerContactPhones.currentContact(customer),
                preferredLanguage,
                now));

        execution.setVariable("ack.emailQueued", queued.emailQueued());
        execution.setVariable("ack.smsQueued", queued.smsQueued());
        execution.setVariable("ack.sent", true);
        execution.setVariable("ack.sentAt", now.toString());

        appendHistory(execution, "Acknowledgment queued for " + ticketId);
    }

    private void appendHistory(DelegateExecution execution, String event) {
        Object historyVar = execution.getVariable(VAR_CASE_HISTORY);
        if (historyVar == null) {
            execution.setVariable(VAR_CASE_HISTORY, event);
        } else {
            execution.setVariable(VAR_CASE_HISTORY, historyVar.toString() + "\n" + event);
        }
    }
}
