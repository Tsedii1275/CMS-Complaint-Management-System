package com.dashenbank.cms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rca_cases")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RcaCase {

    private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_id", unique = true, nullable = false, length = 50)
    private String ticketId;

    @Column(name = "process_instance_id", length = 100)
    private String processInstanceId;

    @Column(name = "root_cause_category", length = 100)
    private String rootCauseCategory;

    @Column(name = "incident_date")
    private LocalDateTime incidentDate;

    @Column(name = "rca_status", nullable = false, length = 30)
    @Builder.Default
    private String rcaStatus = "PENDING"; // PENDING, IN_PROGRESS, COMPLETED

    @Column(name = "analysis_date")
    private LocalDateTime analysisDate;

    @Column(name = "financial_impact", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal financialImpact = BigDecimal.ZERO;

    @Column(name = "reputational_risk", length = 30)
    @Builder.Default
    private String reputationalRisk = "LOW"; // LOW, MEDIUM, HIGH, CRITICAL

    @Column(name = "compliance_impact", length = 100)
    private String complianceImpact;

    @Column(name = "operational_disruption", length = 255)
    private String operationalDisruption;

    @Column(name = "risk_score")
    @Builder.Default
    private Double riskScore = 0.0;

    @Column(name = "preventive_strategy", columnDefinition = "TEXT")
    private String preventiveStrategy;

    @Column(name = "rca_required")
    private Boolean rcaRequired;

    @Column(name = "rca_summary", columnDefinition = "TEXT")
    private String rcaSummary;

    @Column(name = "rca_owner", length = 100)
    private String rcaOwner;

    @Column(name = "rca_completion_date")
    private LocalDateTime rcaCompletionDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now(SYSTEM_ZONE);

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now(SYSTEM_ZONE);

    @OneToOne(mappedBy = "rcaCase", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Rca5Whys whys;

    @OneToMany(mappedBy = "rcaCase", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CapaAction> capaActions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now(SYSTEM_ZONE);
        updatedAt = LocalDateTime.now(SYSTEM_ZONE);
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now(SYSTEM_ZONE);
    }
}
