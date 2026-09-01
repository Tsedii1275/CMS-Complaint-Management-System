package com.dashenbank.cms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Long id;

    @Column(name = "cif_number", unique = true, nullable = false, length = 20)
    private String cifNumber;

    @Column(name = "account_number", unique = true, nullable = false, length = 30)
    private String accountNumber;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "customer_type", nullable = false, length = 50)
    @Builder.Default
    private String customerType = "RETAIL";

    @Column(name = "customer_segment", length = 50)
    private String customerSegment;

    @Column(name = "customer_sub_segment", length = 50)
    private String customerSubSegment;

    @Column(name = "is_vip", nullable = false)
    @Builder.Default
    private boolean isVip = false;

    @Column(name = "risk_rating", length = 20)
    @Builder.Default
    private String riskRating = "LOW";

    @Column(name = "customer_since")
    private LocalDate customerSince;

    @Column(name = "relationship_manager")
    private String relationshipManager;
}
