package com.dashenbank.cms.service;

import com.dashenbank.cms.model.*;
import com.dashenbank.cms.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Production-Ready Data Seeder & Bootstrap Initializer.
 * Creates ONLY the initial bootstrap Administrator account and immutable
 * organizational reference data.
 * All operational users must be created and managed via the Admin Dashboard.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final DistrictRepository districtRepository;
    private final BranchRepository branchRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.initial-password:#{null}}")
    private String configuredAdminPassword;

    public DataSeeder(DistrictRepository districtRepository,
            BranchRepository branchRepository,
            DepartmentRepository departmentRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.districtRepository = districtRepository;
        this.branchRepository = branchRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        bootstrapAdministrator();
        seedHierarchyIfEmpty();
    }

    private void bootstrapAdministrator() {
        var adminOpt = userRepository.findByUsernameIgnoreCase("admin");
        if (adminOpt.isPresent()) {
            log.info(">>> Bootstrap administrator account 'admin' already exists.");
            return;
        }

        String initialPassword = resolveInitialAdminPassword();
        User admin = User.builder()
                .username("admin")
                .email("admin@dashenbank.com")
                .password(passwordEncoder.encode(initialPassword))
                .role(Role.ROLE_ADMIN)
                .authSource(com.dashenbank.cms.model.AuthSource.LOCAL)
                .fullName("System Administrator")
                .department("System Administration")
                .enabled(true)
                .mustChangePassword(true)
                .passwordChangedAt(java.time.LocalDateTime.now())
                .passwordExpiryDate(java.time.LocalDateTime.now().plusDays(
                        com.dashenbank.cms.security.SecurityPolicy.PASSWORD_EXPIRY_DAYS))
                .build();

        userRepository.save(admin);
        log.info(">>> Bootstrap administrator account 'admin' created successfully.");
    }

    private String resolveInitialAdminPassword() {
        String envPassword = System.getenv("INITIAL_ADMIN_PASSWORD");
        String initialPassword = (envPassword != null && !envPassword.isBlank())
                ? envPassword.trim()
                : (configuredAdminPassword == null ? "" : configuredAdminPassword.trim());
        if (initialPassword.isEmpty()) {
            throw new IllegalStateException(
                    "INITIAL_ADMIN_PASSWORD / app.admin.initial-password is required to create the bootstrap admin");
        }
        return initialPassword;
    }

    private void seedHierarchyIfEmpty() {
        if (districtRepository.count() > 0) {
            log.info(">>> Organizational hierarchy reference data already exists. Skipping seeding.");
            return;
        }

        log.info(">>> Seeding initial organizational hierarchy reference data...");

        Map<String, List<BranchSeed>> hierarchy = Map.of(
                "Central District", List.of(
                        new BranchSeed("Bole Branch", "BOLE001", "Branch Manager"),
                        new BranchSeed("Main Branch", "MAIN001", "Branch Manager"),
                        new BranchSeed("Yeka Branch", "YEKA001", "Branch Manager")),
                "Eastern District", List.of(
                        new BranchSeed("Adama Branch", "ADAM001", "Branch Manager"),
                        new BranchSeed("Jimma Branch", "JIMM001", "Branch Manager")),
                "Northern District", List.of(
                        new BranchSeed("Mekelle Branch", "MEKE001", "Branch Manager"),
                        new BranchSeed("Gondar Branch", "GOND001", "Branch Manager")),
                "Southern District", List.of(
                        new BranchSeed("Hawassa Branch", "HAWA001", "Branch Manager"),
                        new BranchSeed("Dilla Branch", "DILL001", "Branch Manager")));

        List<DeptSeed> departments = List.of(
                new DeptSeed("ATM Operations", "Department Manager"),
                new DeptSeed("Digital Banking", "Department Manager"),
                new DeptSeed("Card Operations", "Department Manager"),
                new DeptSeed("Credit Department", "Department Manager"),
                new DeptSeed("Operations Department", "Department Manager"),
                new DeptSeed("Customer Experience", "Department Manager"),
                new DeptSeed("Fraud Investigation", "Department Manager"));

        for (Map.Entry<String, List<BranchSeed>> entry : hierarchy.entrySet()) {
            District district = districtRepository.save(District.builder().name(entry.getKey()).build());

            for (BranchSeed bs : entry.getValue()) {
                Branch branch = branchRepository.save(Branch.builder()
                        .name(bs.name)
                        .code(bs.code)
                        .managerName(bs.managerName)
                        .district(district)
                        .build());

                List<Department> branchDepts = new ArrayList<>();
                for (DeptSeed ds : departments) {
                    Department dept = Department.builder()
                            .name(ds.name)
                            .managerName(ds.managerName)
                            .branch(branch)
                            .build();
                    branchDepts.add(dept);
                }
                departmentRepository.saveAll(branchDepts);
            }
        }
        log.info(">>> Organizational hierarchy reference data seeded successfully.");
    }

    private static class BranchSeed {
        String name;
        String code;
        String managerName;

        BranchSeed(String name, String code, String managerName) {
            this.name = name;
            this.code = code;
            this.managerName = managerName;
        }
    }

    private static class DeptSeed {
        String name;
        String managerName;

        DeptSeed(String name, String managerName) {
            this.name = name;
            this.managerName = managerName;
        }
    }
}
