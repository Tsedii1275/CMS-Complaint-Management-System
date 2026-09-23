package com.dashenbank.cms.notification;

/**
 * Lifecycle of one row in {@code notifications}.
 * PENDING and RETRY are picked up by the dispatcher; SENT, FAILED and SKIPPED are final.
 */
public enum NotificationStatus {
    PENDING,
    SENDING,
    RETRY,
    SENT,
    FAILED,
    SKIPPED
}
