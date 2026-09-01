package com.dashenbank.cms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "complainant_related_information", indexes = {
        @Index(name = "idx_cri_unique_id_no", columnList = "unique_id_no"),
        @Index(name = "idx_cri_case_status", columnList = "case_status"),
        @Index(name = "idx_cri_complaints_category", columnList = "complaints_category"),
        @Index(name = "idx_cri_date_of_complaint", columnList = "date_of_complaint"),
        @Index(name = "idx_cri_district_department", columnList = "district_department"),
        @Index(name = "idx_cri_case_assigned_to", columnList = "case_assigned_to")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplainantRelatedInformation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "unique_id_no", length = 100, nullable = false, unique = true)
    private String uniqueIdNo;

    @Column(name = "date_of_complaint")
    private LocalDateTime dateOfComplaint;

    @Column(name = "name_of_complainant", length = 255)
    private String nameOfComplainant;

    @Column(name = "account_no", length = 50)
    private String accountNo;

    @Column(name = "contact_address", length = 255)
    private String contactAddress;

    @Column(name = "customer_segment", length = 100)
    private String customerSegment;

    @Column(name = "received_by", length = 100)
    private String receivedBy;

    @Column(name = "complaint_made_on_channel", length = 100)
    private String complaintMadeOnChannel;

    @Column(name = "specific_channel_name", length = 100)
    private String specificChannelName;

    @Column(name = "district_department", length = 100)
    private String districtDepartment;

    @Column(name = "service_type", length = 100)
    private String serviceType;

    @Column(name = "details_of_complaint", columnDefinition = "TEXT")
    private String detailsOfComplaint;

    @Column(name = "complaint_classification", length = 100)
    private String complaintClassification;

    @Column(name = "supporting_evidence", length = 255)
    private String supportingEvidence;

    @Column(name = "complainant_acknowledged", length = 50)
    private String complainantAcknowledged;

    @Column(name = "complaint_justified", length = 50)
    private String complaintJustified;

    @Column(name = "validity_reason_for_justified_complaints", columnDefinition = "TEXT")
    private String validityReasonForJustifiedComplaints;

    @Column(name = "nature_of_complaints", length = 255)
    private String natureOfComplaints;

    @Column(name = "complaints_category", length = 100)
    private String complaintsCategory;

    @Column(name = "case_assigned_to", length = 100)
    private String caseAssignedTo;

    @Column(name = "expected_resolution_date")
    private LocalDateTime expectedResolutionDate;

    @Column(name = "actual_resolution_date")
    private LocalDateTime actualResolutionDate;

    @Column(name = "resolution_time_working_days")
    private Integer resolutionTimeWorkingDays;

    @Column(name = "case_forwarded_to", length = 100)
    private String caseForwardedTo;

    @Column(name = "date_case_forwarded")
    private LocalDateTime dateCaseForwarded;

    @Column(name = "reason_for_forwarding", columnDefinition = "TEXT")
    private String reasonForForwarding;

    @Column(name = "case_status", length = 50, nullable = false)
    private String caseStatus;

    @Column(name = "escalated_to", length = 100)
    private String escalatedTo;

    @Column(name = "date_of_escalation")
    private LocalDateTime dateOfEscalation;

    @Column(name = "reason_for_escalation", columnDefinition = "TEXT")
    private String reasonForEscalation;

    @Column(name = "resolution_plan", columnDefinition = "TEXT")
    private String resolutionPlan;

    @Column(name = "resolution_outcome_notified", length = 50)
    private String resolutionOutcomeNotified;

    @Column(name = "means_of_notification", length = 100)
    private String meansOfNotification;

    @Column(name = "complainant_acknowledged_resolution", length = 50)
    private String complainantAcknowledgedResolution;

    @Column(name = "advice_given_to_complainant", columnDefinition = "TEXT")
    private String adviceGivenToComplainant;

    @Column(name = "customer_reaction_to_handling_process", length = 100)
    private String customerReactionToHandlingProcess;

    @Column(name = "customer_lifetime_value", length = 100)
    private String customerLifetimeValue;

    @Column(name = "remark_and_special_note", columnDefinition = "TEXT")
    private String remarkAndSpecialNote;

    @Column(name = "requires_follow_up", length = 50)
    private String requiresFollowUp;

    @Column(name = "latest_status_and_remark", columnDefinition = "TEXT")
    private String latestStatusAndRemark;

    @Column(name = "is_manually_edited", nullable = false)
    private Boolean isManuallyEdited;

    @Column(name = "last_edited_by", length = 100)
    private String lastEditedBy;

    @Column(name = "last_edited_at")
    private LocalDateTime lastEditedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
