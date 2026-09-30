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

/**
 * Production-Ready Data Seeder & Bootstrap Initializer.
 * Creates ONLY the initial bootstrap Administrator account.
 * All operational users must be created and managed via the Admin Dashboard.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.initial-password:#{null}}")
    private String configuredAdminPassword;

    public DataSeeder(UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        bootstrapAdministrator();
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
                .approved(true)
                .approvalStatus("APPROVED")
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
}
