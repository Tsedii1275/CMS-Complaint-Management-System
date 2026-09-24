package com.dashenbank.cms.notification.provider;

import com.dashenbank.cms.notification.NotificationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GmailSmtpGatewayTest {

    @Test
    void detectsGmailRecipients() {
        assertTrue(GmailSmtpGateway.isGmailAddress("tsedi@gmail.com"));
        assertTrue(GmailSmtpGateway.isGmailAddress("User@GoogleMail.com"));
        assertFalse(GmailSmtpGateway.isGmailAddress("Tsedey.TekaBetela@dashenbanksc.com"));
        assertFalse(GmailSmtpGateway.isGmailAddress(""));
    }

    @Test
    void handlesOnlyWhenEnabledAndConfigured() {
        NotificationProperties properties = new NotificationProperties();
        GmailSmtpGateway gateway = new GmailSmtpGateway(properties);
        assertFalse(gateway.handles("a@gmail.com"));

        properties.getEmail().getGmail().setEnabled(true);
        properties.getEmail().getGmail().setUsername("cms@gmail.com");
        properties.getEmail().getGmail().setPassword("app-password");
        properties.getEmail().getGmail().setFrom("cms@gmail.com");
        assertTrue(gateway.handles("a@gmail.com"));
        assertFalse(gateway.handles("a@dashenbanksc.com"));
    }

    @Test
    void enabledWithoutSecretsFailsValidation() {
        NotificationProperties properties = new NotificationProperties();
        properties.getEmail().getGmail().setEnabled(true);
        GmailSmtpGateway gateway = new GmailSmtpGateway(properties);
        assertThrows(IllegalStateException.class, gateway::validateIfEnabled);
    }

    @Test
    void routesGmailRecipientToGmailGateway() {
        NotificationProperties properties = new NotificationProperties();
        properties.getEmail().setFrom("info@dashenbanksc.com");
        GmailSmtpGateway gmail = mock(GmailSmtpGateway.class);
        when(gmail.handles("tsedi@gmail.com")).thenReturn(true);
        when(gmail.send(org.mockito.ArgumentMatchers.any())).thenReturn(ProviderResult.delivered("gmail-1"));

        SmtpEmailProvider smtp = new SmtpEmailProvider(mailSender(), new MockEnvironment(), properties, gmail);
        ProviderResult result = smtp.send(new EmailMessage("tsedi@gmail.com", "info@dashenbanksc.com",
                "Customer Service Team", "info@dashenbanksc.com", "Hi", "Body"));
        assertEquals(ProviderResult.Outcome.DELIVERED, result.outcome());
        assertEquals("gmail-1", result.providerMessageId());
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<JavaMailSender> mailSender() {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(mock(JavaMailSender.class));
        return provider;
    }
}
