package com.dashenbank.cms.notification.provider;

import com.dashenbank.cms.notification.NotificationChannel;
import com.dashenbank.cms.notification.NotificationProperties;
import com.dashenbank.cms.notification.provider.datapower.DataPowerSmsProvider;
import com.dashenbank.cms.notification.provider.datapower.SmsMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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

    @Test
    void datapowerWithoutCredentialsFailsStartupOnlyWhenSmsIsEnabled() {
        NotificationProperties properties = new NotificationProperties();
        properties.getSms().setProvider(DataPowerSmsProvider.ID);
        NotificationProperties.DataPower config = properties.getSms().getDatapower();
        config.setTokenUrl("https://datapower-cp4iuat.dashenbanksc.com:9443/dashen-bank/sandbox/oauth19/oauth2/token");
        config.setSendUrl("https://datapower-cp4iuat.dashenbanksc.com:9443/dashen-bank/sandbox/SMS/send");
        config.setScope("DASHEN");
        config.setSendFor("Voice Management");
        config.setPhoneFormat("LOCAL_09");
        config.setAllowedLocalPrefixes("09");
        DataPowerSmsProvider datapower = new DataPowerSmsProvider(properties, new SmsMetrics(new SimpleMeterRegistry()));

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> new NotificationProviderRegistry(
                List.of(new LoggingEmailProvider()), List.of(datapower, new LoggingSmsProvider()), properties));
        assertTrue(error.getMessage().contains("SMS_DATAPOWER_CLIENT_ID"));

        properties.getSms().setEnabled(false);
        NotificationProviderRegistry registry = new NotificationProviderRegistry(
                List.of(new LoggingEmailProvider()), List.of(datapower, new LoggingSmsProvider()), properties);
        assertEquals("datapower", registry.providerId(NotificationChannel.SMS));
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<JavaMailSender> mailSender() {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(mock(JavaMailSender.class));
        return provider;
    }
}
