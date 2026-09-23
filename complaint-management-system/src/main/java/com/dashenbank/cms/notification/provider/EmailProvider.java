package com.dashenbank.cms.notification.provider;

/**
 * Email transport. Register an implementation as a Spring bean and select it
 * with {@code NOTIFICATION_EMAIL_PROVIDER=<id>}; business code talks only to
 * {@code NotificationService} and does not change when the transport does.
 *
 * <p>Implementations are called from dispatcher worker threads and must be
 * thread-safe. They must not throw for delivery problems: classify them in the
 * returned {@link ProviderResult} so the dispatcher can retry or give up.
 */
public interface EmailProvider {

    String id();

    /**
     * Called once at startup when this provider is selected and email is
     * enabled. Throw {@link IllegalStateException} for missing configuration.
     */
    default void validateConfiguration() {
    }

    ProviderResult send(EmailMessage message);
}
