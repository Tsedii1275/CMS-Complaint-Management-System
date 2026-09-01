package com.dashenbank.cms.delegate;

import com.dashenbank.cms.service.AuditService;
import com.dashenbank.cms.service.NotificationService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component("generateTicketDelegate")
public class GenerateTicketDelegate implements JavaDelegate {

    private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();
    private static final String KEY_CASE_HISTORY = "caseHistory";
    private static final String LANG_AMHARIC = "amharic";

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
        sla.put("escalationLevel", 0);
        execution.setVariable("sla", sla);
    }

    private void sendRegistrationConfirmationIfNeeded(DelegateExecution execution, String ticket) {
        Boolean ackSent = (Boolean) execution.getVariable("ack.sent");
        Boolean ticketEmailSent = (Boolean) execution.getVariable("notification.ticketEmailSent");

        // Per requirement: Only ONE Complaint Registration Confirmation notification
        // per complaint lifecycle
        if (Boolean.TRUE.equals(ackSent) || Boolean.TRUE.equals(ticketEmailSent)) {
            return;
        }

        Map<String, Object> customer = castToMap(execution.getVariable("customer"));
        String customerName = (String) customer.getOrDefault("name", "Valued Customer");
        String email = (String) customer.get("email");
        String phone = (String) customer.get("phone");
        String preferredLanguage = resolvePreferredLanguage(execution, customer);

        LocalDateTime now = LocalDateTime.now(SYSTEM_ZONE);
        String subDate = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String subTime = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        NotificationCopy copy = buildRegistrationCopy(preferredLanguage, customerName, subDate, subTime, ticket);

        if (email != null && !email.isBlank()) {
            notificationService.sendEmail(email, copy.subject, copy.emailMessage);
            execution.setVariable("notification.ticketEmailSent", true);
        }
        if (phone != null && !phone.isBlank()) {
            notificationService.sendSms(phone, copy.emailMessage);
            execution.setVariable("notification.ticketSmsSent", true);
        }
        execution.setVariable("ack.sent", true);
    }

    private String resolvePreferredLanguage(DelegateExecution execution, Map<String, Object> customer) {
        String preferredLanguage = (String) execution.getVariable("preferredLanguage");
        if (preferredLanguage == null && !customer.isEmpty()) {
            preferredLanguage = (String) customer.getOrDefault("preferredLanguage", "english");
        }
        return preferredLanguage;
    }

    private NotificationCopy buildRegistrationCopy(String preferredLanguage, String customerName, String subDate,
            String subTime, String ticket) {
        if (LANG_AMHARIC.equalsIgnoreCase(preferredLanguage)) {
            return new NotificationCopy(
                    "የቅሬታ ምዝገባ ማረጋገጫ",
                    String.format(
                            "ውድ %s፣%n%n" +
                                    "በ %s በ %s ያቀረቡት ቅሬታ በተሳካ ሁኔታ ተመዝግቧል።%n%n" +
                                    "የቲኬት ቁጥር: %s%n%n" +
                                    "ቅሬታዎ በአሁኑ ጊዜ እየተገመገመ እና በሂደት ላይ ይገኛል። ከላይ ያለውን የቲኬት ቁጥር በመጠቀም ሂደቱን መከታተል ይችላሉ።%n%n" +
                                    "ዳሽን ባንክን ስላነጋገሩ እናመሰግናለን።%n%n" +
                                    "ዳሽን ባንክ%n" +
                                    "የደንበኞች አገልግሎት ቡድን",
                            customerName, subDate, subTime, ticket));
        }
        return new NotificationCopy(
                "Complaint Registration Confirmation",
                String.format(
                        "Dear %s,%n%n" +
                                "Your complaint submitted on %s at %s has been successfully registered.%n%n" +
                                "Ticket ID: %s%n%n" +
                                "Your complaint is currently under review and processing. You may track its progress using the ticket ID above.%n%n"
                                +
                                "Thank you for contacting Dashen Bank.%n%n" +
                                "Dashen Bank%n" +
                                "Customer Care Team",
                        customerName, subDate, subTime, ticket));
    }

    private void appendHistory(DelegateExecution execution, String event) {
        Object historyVar = execution.getVariable(KEY_CASE_HISTORY);
        if (historyVar == null) {
            execution.setVariable(KEY_CASE_HISTORY, event);
        } else {
            execution.setVariable(KEY_CASE_HISTORY, historyVar.toString() + "\n" + event);
        }
    }

    private record NotificationCopy(String subject, String emailMessage) {
    }
}
