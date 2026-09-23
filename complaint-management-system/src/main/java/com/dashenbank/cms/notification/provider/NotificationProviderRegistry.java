package com.dashenbank.cms.notification.provider;

import com.dashenbank.cms.notification.NotificationChannel;
import com.dashenbank.cms.notification.NotificationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Resolves the configured email and SMS providers at startup and fails fast on
 * an unknown provider id or incomplete provider configuration.
 */
@Component
public class NotificationProviderRegistry {

    private static final Logger log = LoggerFactory.getLogger(NotificationProviderRegistry.class);

    private final EmailProvider emailProvider;
    private final SmsProvider smsProvider;

    public NotificationProviderRegistry(List<EmailProvider> emailProviders, List<SmsProvider> smsProviders,
            NotificationProperties properties) {
        this.emailProvider = select(index(emailProviders, EmailProvider::id, "email"),
                properties.getEmail().getProvider(), "NOTIFICATION_EMAIL_PROVIDER");
        this.smsProvider = select(index(smsProviders, SmsProvider::id, "SMS"),
                properties.getSms().getProvider(), "NOTIFICATION_SMS_PROVIDER");

        boolean emailActive = properties.isEnabled() && properties.getEmail().isEnabled();
        boolean smsActive = properties.isEnabled() && properties.getSms().isEnabled();
        if (emailActive) {
            emailProvider.validateConfiguration();
        }
        if (smsActive) {
            smsProvider.validateConfiguration();
        }
        log.info("Notification providers: email={} (enabled={}), sms={} (enabled={})",
                emailProvider.id(), emailActive, smsProvider.id(), smsActive);
    }

    public EmailProvider emailProvider() {
        return emailProvider;
    }

    public SmsProvider smsProvider() {
        return smsProvider;
    }

    public String providerId(NotificationChannel channel) {
        return channel == NotificationChannel.EMAIL ? emailProvider.id() : smsProvider.id();
    }

    private static <T> Map<String, T> index(List<T> providers, Function<T, String> idOf, String kind) {
        Map<String, T> byId = new TreeMap<>();
        for (T provider : providers) {
            String id = idOf.apply(provider).trim().toLowerCase(Locale.ROOT);
            if (byId.putIfAbsent(id, provider) != null) {
                throw new IllegalStateException("Duplicate " + kind + " provider id '" + id + "'");
            }
        }
        return byId;
    }

    private static <T> T select(Map<String, T> byId, String configured, String envName) {
        String id = configured == null ? "" : configured.trim().toLowerCase(Locale.ROOT);
        T provider = byId.get(id);
        if (provider == null) {
            throw new IllegalStateException(envName + "='" + configured + "' is not a known provider. Available: "
                    + String.join(", ", byId.keySet()));
        }
        return provider;
    }
}
