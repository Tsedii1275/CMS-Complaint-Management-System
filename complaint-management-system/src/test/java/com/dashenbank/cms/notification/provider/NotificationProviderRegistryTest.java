package com.dashenbank.cms.notification.provider;

import com.dashenbank.cms.notification.NotificationChannel;
import com.dashenbank.cms.notification.NotificationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationProviderRegistryTest {

    @Test
    void defaultsToNonDeliveringProviders() {
        NotificationProviderRegistry registry = new NotificationProviderRegistry(
                List.of(new LoggingEmailProvider()), List.of(new LoggingSmsProvider()), new NotificationProperties());

        assertEquals("log", registry.providerId(NotificationChannel.EMAIL));
        assertEquals("log", registry.providerId(NotificationChannel.SMS));
    }

    @Test
    void unknownProviderFailsStartup() {
        NotificationProperties properties = new NotificationProperties();
        properties.getSms().setProvider("ethiotelecom");

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> new NotificationProviderRegistry(
                List.of(new LoggingEmailProvider()), List.of(new LoggingSmsProvider()), properties));
        assertTrue(error.getMessage().contains("NOTIFICATION_SMS_PROVIDER"));
    }

    @Test
    void smtpWithoutHostFailsStartupOnlyWhenEmailIsEnabled() {
        NotificationProperties properties = new NotificationProperties();
        properties.getEmail().setProvider("smtp");
        SmtpEmailProvider smtp = new SmtpEmailProvider(mailSender(), new MockEnvironment(), properties);

        assertThrows(IllegalStateException.class, () -> new NotificationProviderRegistry(
                List.of(smtp, new LoggingEmailProvider()), List.of(new LoggingSmsProvider()), properties));

        properties.getEmail().setEnabled(false);
        assertDoesNotThrow(() -> new NotificationProviderRegistry(
                List.of(smtp, new LoggingEmailProvider()), List.of(new LoggingSmsProvider()), properties));
    }

    @Test
    void smtpWithHostAndSenderIsAccepted() {
        NotificationProperties properties = new NotificationProperties();
        properties.getEmail().setProvider("smtp");
        properties.getEmail().setFrom("customercare@dashenbanksc.com");
        MockEnvironment environment = new MockEnvironment().withProperty("spring.mail.host", "relay.dashenbank.local");
        SmtpEmailProvider smtp = new SmtpEmailProvider(mailSender(), environment, properties);

        NotificationProviderRegistry registry = new NotificationProviderRegistry(
                List.of(smtp, new LoggingEmailProvider()), List.of(new LoggingSmsProvider()), properties);

        assertEquals("smtp", registry.providerId(NotificationChannel.EMAIL));
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<JavaMailSender> mailSender() {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(mock(JavaMailSender.class));
        return provider;
    }
}
