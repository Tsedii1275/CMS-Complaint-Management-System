package com.dashenbank.cms.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "corrective_preventive_actions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CapaAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rca_case_id", nullable = false)
    @JsonIgnore
    private RcaCase rcaCase;

    @Column(name = "action_type", nullable = false, length = 20)
    private String actionType; // CORRECTIVE, PREVENTIVE

    @Column(name = "action_description", nullable = false, columnDefinition = "TEXT")
    private String actionDescription;

    @Column(nullable = false, length = 100)
    private String owner;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(name = "implementation_status", nullable = false, length = 30)
    @Builder.Default
    private String implementationStatus = "PENDING"; // PENDING, IN_PROGRESS, COMPLETED, OVERDUE

    @Column(name = "effectiveness_rating", length = 20)
    private String effectivenessRating; // POOR, SATISFACTORY, EXCELLENT

    @Column(name = "verification_notes", columnDefinition = "TEXT")
    private String verificationNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
