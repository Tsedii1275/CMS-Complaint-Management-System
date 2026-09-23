package com.dashenbank.cms.notification;

/**
 * Whether each channel was queued for delivery. Queued is not delivered: the
 * delivery outcome is recorded later in {@code notifications} and {@code audit_log}.
 */
public record NotificationQueueResult(boolean emailQueued, boolean smsQueued) {

    public static final NotificationQueueResult NONE = new NotificationQueueResult(false, false);
}
