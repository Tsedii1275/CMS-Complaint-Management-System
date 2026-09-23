package com.dashenbank.cms.notification;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Template variable names and request factories shared by the controller and
 * Flowable delegates.
 */
public final class CustomerNotifications {

    public static final String CUSTOMER_NAME = "customerName";
    public static final String TICKET_ID = "ticketId";
    public static final String SUBMISSION_DATE = "submissionDate";
    public static final String SUBMISSION_TIME = "submissionTime";
    public static final String RESOLUTION_DATE = "resolutionDate";
    public static final String RESOLUTION_SUMMARY = "resolutionSummary";
    public static final String FEEDBACK_LINK = "feedbackLink";

    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private CustomerNotifications() {
    }

    @SuppressWarnings("java:S107")
    public static NotificationRequest registration(String ticketId, String processInstanceId, String customerName,
            String email, String phone, String preferredLanguage, LocalDateTime submittedAt) {
        return NotificationRequest.builder()
                .eventType(NotificationEventType.COMPLAINT_REGISTERED)
                .complaintRef(ticketId)
                .processInstanceId(processInstanceId)
                .preferredLanguage(preferredLanguage)
                .email(email)
                .phone(phone)
                .variable(CUSTOMER_NAME, customerName)
                .variable(TICKET_ID, ticketId)
                .variable(SUBMISSION_DATE, submittedAt.format(DATE))
                .variable(SUBMISSION_TIME, submittedAt.format(TIME))
                .build();
    }
}
