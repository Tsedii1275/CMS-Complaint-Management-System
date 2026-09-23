package com.dashenbank.cms.notification;

import lombok.Builder;
import lombok.Singular;

import java.util.Map;

/**
 * What business code asks for. Channel selection, templates, providers,
 * retries and auditing are handled by {@link NotificationService}.
 *
 * @param complaintRef    ticket shown to the customer (DBC-/CM- id); also the audit key
 * @param email           raw email; blank skips the email channel
 * @param phone           raw current contact phone; blank skips the SMS channel
 * @param variables       template placeholder values, see {@link CustomerNotifications}
 * @param occurrenceKey   distinguishes legitimate repeats of the same event for the same
 *                        complaint (for example a new feedback token after a reopen);
 *                        {@code null} means the event is sent at most once per channel
 */
@Builder
public record NotificationRequest(
        NotificationEventType eventType,
        String complaintRef,
        String processInstanceId,
        String preferredLanguage,
        String email,
        String phone,
        @Singular Map<String, String> variables,
        String occurrenceKey) {
}
