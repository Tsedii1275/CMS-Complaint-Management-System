package com.dashenbank.cms.notification.provider;

/**
 * SMS transport. Register an implementation as a Spring bean and select it
 * with {@code NOTIFICATION_SMS_PROVIDER=<id>}; gateway connection settings are
 * available from {@code NotificationProperties.getSms().getGateway()}.
 *
 * <p>Implementations are called from dispatcher worker threads and must be
 * thread-safe. They must not throw for delivery problems: classify them in the
 * returned {@link ProviderResult} so the dispatcher can retry or give up.
 */
public interface SmsProvider {

    String id();

    /**
     * Called once at startup when this provider is selected and SMS is
     * enabled. Throw {@link IllegalStateException} for missing configuration.
     */
    default void validateConfiguration() {
    }

    ProviderResult send(SmsMessage message);
}
