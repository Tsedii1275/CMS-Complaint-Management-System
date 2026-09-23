package com.dashenbank.cms.notification;

public enum NotificationChannel {
    EMAIL("email"),
    SMS("sms");

    private final String templateSegment;

    NotificationChannel(String templateSegment) {
        this.templateSegment = templateSegment;
    }

    public String templateSegment() {
        return templateSegment;
    }
}
