package com.dashenbank.cms.notification;

/**
 * Customer-facing notification events. {@link #templateKey()} is the prefix of
 * the keys in {@code notification/templates/messages_<lang>.properties}.
 */
public enum NotificationEventType {
    COMPLAINT_REGISTERED("registration"),
    COMPLAINT_RESOLVED("resolution"),
    FCR_RESOLVED("resolution");

    private final String templateKey;

    NotificationEventType(String templateKey) {
        this.templateKey = templateKey;
    }

    public String templateKey() {
        return templateKey;
    }
}
