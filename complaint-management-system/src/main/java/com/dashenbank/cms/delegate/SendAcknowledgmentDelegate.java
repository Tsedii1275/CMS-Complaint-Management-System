package com.dashenbank.cms.delegate;

import com.dashenbank.cms.service.NotificationService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component("sendAcknowledgmentDelegate")
public class SendAcknowledgmentDelegate implements JavaDelegate {

    private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();
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
        String email = (String) customer.get("email");
        String phone = (String) customer.get("phone");

        String customerName = (String) customer.getOrDefault("name", "Valued Customer");

        String preferredLanguage = (String) execution.getVariable("preferredLanguage");
        if (preferredLanguage == null && !customer.isEmpty()) {
            preferredLanguage = (String) customer.getOrDefault("preferredLanguage", "english");
        }

        LocalDateTime now = LocalDateTime.now(SYSTEM_ZONE);
        String subDate = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String subTime = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        String message;
        String subject;

        if ("amharic".equalsIgnoreCase(preferredLanguage)) {
            subject = "የቅሬታ ምዝገባ ማረጋገጫ";
            message = String.format(
                    "ውድ %s፣%n%n" +
                            "በ %s በ %s ያቀረቡት ቅሬታ በተሳካ ሁኔታ ተመዝግቧል።%n%n" +
                            "የቲኬት ቁጥር: %s%n%n" +
                            "ቅሬታዎ በአሁኑ ጊዜ እየተገመገመ እና በሂደት ላይ ይገኛል። ከላይ ያለውን የቲኬት ቁጥር በመጠቀም ሂደቱን መከታተል ይችላሉ።%n%n" +
                            "ዳሽን ባንክን ስላነጋገሩ እናመሰግናለን።%n%n" +
                            "ዳሽን ባንክ%n" +
                            "የደንበኞች አገልግሎት ቡድን",
                    customerName, subDate, subTime, ticketId);
        } else {
            subject = "Complaint Registration Confirmation";
            message = String.format(
                    "Dear %s,%n%n" +
                            "Your complaint submitted on %s at %s has been successfully registered.%n%n" +
                            "Ticket ID: %s%n%n" +
                            "Your complaint is currently under review and processing. You may track its progress using the ticket ID above.%n%n"
                            +
                            "Thank you for contacting Dashen Bank.%n%n" +
                            "Dashen Bank%n" +
                            "Customer Care Team",
                    customerName, subDate, subTime, ticketId);
        }

        if (email != null && !email.isBlank()) {
            notificationService.sendEmail(email, subject, message);
            execution.setVariable("ack.emailSent", true);
        }
        if (phone != null && !phone.isBlank()) {
            notificationService.sendSms(phone, message);
            execution.setVariable("ack.smsSent", true);
        }

        execution.setVariable("ack.sent", true);
        execution.setVariable("ack.sentAt", now.toString());

        appendHistory(execution, "Acknowledgment sent for " + ticketId);
    }

    private void appendHistory(DelegateExecution execution, String event) {
        Object historyVar = execution.getVariable("caseHistory");
        if (historyVar == null) {
            execution.setVariable("caseHistory", event);
        } else {
            execution.setVariable("caseHistory", historyVar.toString() + "\n" + event);
        }
    }
}
