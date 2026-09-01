package com.dashenbank.cms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "first_contact_resolutions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FirstContactResolution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "complaint_id", length = 100)
    private String complaintId;

    @Column(name = "process_instance_id", length = 100)
    private String processInstanceId;

    @Column(name = "customer_name", length = 255)
    private String customerName;

    @Column(name = "account_number", length = 100)
    private String accountNumber;

    @Column(name = "complaint_category", length = 100)
    private String complaintCategory;

    @Column(name = "fcr_notes", columnDefinition = "TEXT")
    private String fcrNotes;
}
