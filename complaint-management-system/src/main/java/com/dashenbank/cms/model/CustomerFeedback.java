package com.dashenbank.cms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_feedback")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "complaint_id", length = 100)
    private String complaintId;

    @Column(name = "ticket_number", length = 100, nullable = false)
    private String ticketNumber;

    @Column(name = "resolution_confirmed")
    private Boolean resolutionConfirmed;

    @Column(name = "csat_score")
    private Integer csatScore;

    @Column(name = "nps_score")
    private Integer npsScore;

    @Column(name = "nps_comment", columnDefinition = "TEXT")
    private String npsComment;

    @Column(name = "ces_score")
    private Integer cesScore;

    @Column(name = "ces_comment", columnDefinition = "TEXT")
    private String cesComment;

    @Column(name = "additional_comments", columnDefinition = "TEXT")
    private String additionalComments;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "reopened_case")
    @Builder.Default
    private Boolean reopenedCase = false;

    @Column(name = "secure_token", length = 100, unique = true)
    private String secureToken;

    @Column(name = "token_expired")
    @Builder.Default
    private Boolean tokenExpired = false;

    @Column(name = "feedback_request_sent_at")
    private LocalDateTime feedbackRequestSentAt;

    @Column(name = "feedback_submitted_at")
    private LocalDateTime feedbackSubmittedAt;

    @Column(name = "feedback_response_time")
    private Long feedbackResponseTime;

    @Column(name = "reopen_count")
    @Builder.Default
    private Integer reopenCount = 0;

    @Column(name = "customer_satisfaction_status", length = 30)
    private String customerSatisfactionStatus;

    @Column(name = "preferred_language", length = 30)
    @Builder.Default
    private String preferredLanguage = "english";

    @Column(name = "employee_id", length = 100)
    private String employeeId;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "resolution_speed_rating", length = 50)
    private String resolutionSpeedRating;

    @Column(name = "ease_rating", length = 50)
    private String easeRating;

    @Column(name = "followup_status", length = 50)
    @Builder.Default
    private String followupStatus = "PENDING"; // PENDING, IN_PROGRESS, CLOSED

    @Column(name = "followup_started_at")
    private LocalDateTime followupStartedAt;

    @Column(name = "followup_closed_at")
    private LocalDateTime followupClosedAt;

    @Column(name = "followup_assigned_to", length = 100)
    private String followupAssignedTo;

    // Compatibility getters & setters to avoid breaking other Java files:
    public String getTicketId() {
        return this.ticketNumber;
    }

    public void setTicketId(String ticketId) {
        this.ticketNumber = ticketId;
    }

    public String getProcessInstanceId() {
        return this.complaintId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.complaintId = processInstanceId;
    }

    public Boolean getSatisfied() {
        return this.resolutionConfirmed;
    }

    public void setSatisfied(Boolean satisfied) {
        this.resolutionConfirmed = satisfied;
    }

    public String getComment() {
        return this.additionalComments;
    }

    public void setComment(String comment) {
        this.additionalComments = comment;
    }
}
