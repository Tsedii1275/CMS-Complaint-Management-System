package com.dashenbank.cms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "action", length = 100)
    private String action;

    @Column(name = "actor", length = 100)
    private String actor;

    @Column(name = "actor_id", length = 100)
    private String actorId;

    @Column(name = "complaint_id", length = 100)
    private String complaintId;

    @Column(name = "general_ticket_id", length = 50)
    private String generalTicketId;

    @Column(name = "customer_name", length = 100)
    private String customerName;

    @Column(name = "details_of_complaint", columnDefinition = "TEXT")
    private String detailsOfComplaint;

    @Column(name = "complaint_category", length = 100)
    private String complaintCategory;

    @Column(name = "process_instance_id", length = 100)
    private String processInstanceId;

    @Column(name = "task_id", length = 100)
    private String taskId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
