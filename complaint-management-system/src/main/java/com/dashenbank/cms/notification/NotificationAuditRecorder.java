package com.dashenbank.cms.notification;

import com.dashenbank.cms.service.AuditService;
import org.springframework.stereotype.Component;

/**
 * Writes final notification outcomes to {@code audit_log}. Recipients are
 * masked; full delivery detail stays in {@code notifications} (see the id).
 */
@Component
public class NotificationAuditRecorder {

    public static final String ACTION_SENT = "NOTIFICATION_SENT";
    public static final String ACTION_FAILED = "NOTIFICATION_FAILED";
    public static final String ACTION_SKIPPED = "NOTIFICATION_SKIPPED";
    public static final String ACTION_RETRY_REQUESTED = "NOTIFICATION_RETRY_REQUESTED";
    private static final String ACTOR = "system";
    private static final String ACTOR_ID = "notification-service";

    private final AuditService auditService;

    public NotificationAuditRecorder(AuditService auditService) {
        this.auditService = auditService;
    }

    public void recordOutcome(NotificationRecord notification) {
        String action = switch (notification.getStatus()) {
            case SENT -> ACTION_SENT;
            case FAILED -> ACTION_FAILED;
            case SKIPPED -> ACTION_SKIPPED;
            default -> null;
        };
        if (action == null) {
            return;
        }
        String verb = switch (notification.getStatus()) {
            case SENT -> "delivered";
            case FAILED -> "failed";
            default -> "not delivered";
        };
        auditService.log(notification.getComplaintRef(), notification.getProcessInstanceId(), null, action, ACTOR,
                ACTOR_ID, describe(notification, verb));
    }

    public void recordRetryRequested(NotificationRecord notification, String requestedBy) {
        auditService.log(notification.getComplaintRef(), notification.getProcessInstanceId(), null,
                ACTION_RETRY_REQUESTED, "admin", requestedBy, describe(notification, "re-queued by " + requestedBy));
    }

    private static String describe(NotificationRecord notification, String verb) {
        String provider = notification.getProvider() == null ? "n/a" : notification.getProvider();
        return String.format("%s %s notification #%d %s for %s via %s (attempt %d).",
                notification.getChannel(), notification.getEventType(), notification.getId(), verb,
                RecipientFormat.mask(notification.getChannel(), notification.getRecipient()), provider,
                notification.getAttempts());
    }
}
