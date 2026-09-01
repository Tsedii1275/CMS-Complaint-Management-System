package com.dashenbank.cms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "rca_audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RcaAuditLog {

    private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rca_case_id")
    private Long rcaCaseId;

    @Column(name = "ticket_id", length = 50)
    private String ticketId;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(nullable = false, length = 100)
    private String actor;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now(SYSTEM_ZONE);

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now(SYSTEM_ZONE);
    }
}
