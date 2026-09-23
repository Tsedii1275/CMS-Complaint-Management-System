package com.dashenbank.cms.notification;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationTemplateServiceTest {

    private final NotificationTemplateService templates =
            new NotificationTemplateService(new DefaultResourceLoader(), new NotificationProperties());

    @Test
    void rendersEnglishRegistrationEmail() {
        RenderedMessage rendered = templates.render(NotificationEventType.COMPLAINT_REGISTERED,
                NotificationChannel.EMAIL, "english", Map.of(
                        CustomerNotifications.CUSTOMER_NAME, "Abebe",
                        CustomerNotifications.TICKET_ID, "DBC-2026-000001",
                        CustomerNotifications.SUBMISSION_DATE, "2026-09-23",
                        CustomerNotifications.SUBMISSION_TIME, "14:00:00"));

        assertEquals("Complaint Registration Confirmation", rendered.subject());
        assertTrue(rendered.body().startsWith("Dear Abebe,\n\nYour complaint submitted on 2026-09-23 at 14:00:00"));
        assertTrue(rendered.body().contains("Ticket ID: DBC-2026-000001"));
        assertTrue(rendered.body().endsWith("Dashen Bank\nCustomer Care Team"));
    }

    @Test
    void rendersAmharicResolutionAndShortSms() {
        Map<String, String> vars = Map.of(
                CustomerNotifications.CUSTOMER_NAME, "አበበ",
                CustomerNotifications.TICKET_ID, "DBC-1",
                CustomerNotifications.FEEDBACK_LINK, "https://cms/f?token=t");

        RenderedMessage email = templates.render(NotificationEventType.COMPLAINT_RESOLVED,
                NotificationChannel.EMAIL, "amharic", vars);
        RenderedMessage sms = templates.render(NotificationEventType.COMPLAINT_RESOLVED,
                NotificationChannel.SMS, "am", vars);

        assertEquals("የቅሬታ መፍትሄ መረጃ", email.subject());
        assertTrue(email.body().startsWith("ውድ አበበ፣"));
        assertNull(sms.subject());
        assertTrue(sms.body().contains("DBC-1"));
        assertTrue(sms.body().contains("https://cms/f?token=t"));
        assertTrue(sms.body().length() < email.body().length());
    }

    @Test
    void doesNotReinterpretVariableValues() {
        String filled = NotificationTemplateService.fill("Summary: {resolutionSummary}",
                Map.of("resolutionSummary", "Refund $100 {ticketId} \\done"));

        assertEquals("Summary: Refund $100 {ticketId} \\done", filled);
    }

    @Test
    void missingVariablesRenderEmpty() {
        assertEquals("Hi !", NotificationTemplateService.fill("Hi {customerName}!", Map.of()));
    }

    @Test
    void failsFastWhenTemplateDirectoryIsMissing() {
        NotificationProperties properties = new NotificationProperties();
        properties.getTemplates().setLocation("classpath:no-such-dir/");

        assertThrows(IllegalStateException.class,
                () -> new NotificationTemplateService(new DefaultResourceLoader(), properties));
    }
}
