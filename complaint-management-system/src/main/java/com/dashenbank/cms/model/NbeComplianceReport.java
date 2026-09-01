package com.dashenbank.cms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Regulatory store for NBE Annex 1 / Annex 2 reporting fields.
 *
 * <p>
 * Customer and complaint identity is not duplicated here. Those values are
 * loaded from {@code complainant_related_information} and
 * {@code complaint_sla_metrics} when a report is generated. The business key is
 * {@code ticket_number}; {@code process_instance_id} is an optional correlation
 * id only and is not a Flowable foreign key.
 */
@Entity
@Table(name = "nbe_compliance_reports", indexes = {
        @Index(name = "idx_nbe_process_instance_id", columnList = "process_instance_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NbeComplianceReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_number", length = 100, nullable = false, unique = true)
    private String ticketNumber;

    @Column(name = "process_instance_id", length = 64)
    private String processInstanceId;

    @Column(name = "report_status", length = 100)
    private String reportStatus;

    @Column(name = "staff_handling", length = 255)
    private String staffHandling;

    @Column(name = "days_open")
    private Integer daysOpen;

    @Column(name = "reason_for_non_resolution", columnDefinition = "TEXT")
    private String reasonForNonResolution;

    @Column(name = "additional_comments", columnDefinition = "TEXT")
    private String additionalComments;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
