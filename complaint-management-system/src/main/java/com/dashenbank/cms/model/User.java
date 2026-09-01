package com.dashenbank.cms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Convert(converter = RoleConverter.class)
    @Column(name = "role", nullable = false, length = 100)
    private Role role;

    @Builder.Default
    private boolean enabled = true;

    @Builder.Default
    @Column(name = "must_change_password")
    private boolean mustChangePassword = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "district")
    private String district;

    @Column(name = "branch")
    private String branch;

    @Column(name = "department")
    private String department;

    @Column(name = "full_name")
    private String fullName;
}
