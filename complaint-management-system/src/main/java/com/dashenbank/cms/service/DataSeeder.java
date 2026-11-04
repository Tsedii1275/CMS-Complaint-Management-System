package com.dashenbank.cms.service;

import com.dashenbank.cms.model.AuthSource;
import com.dashenbank.cms.model.Role;
import com.dashenbank.cms.model.User;
import com.dashenbank.cms.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds the System Administrator ({@code admin}) and Operational Admin ({@code opadmin})
 * when those usernames are missing. Existing user-management accounts are never deleted.
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
        bootstrapSystemAdministrator();
        bootstrapOperationalAdministrator();
    }

    private void bootstrapSystemAdministrator() {
        var adminOpt = userRepository.findByUsernameIgnoreCase("admin");
        if (adminOpt.isPresent()) {
            User admin = adminOpt.get();
            boolean changed = false;
            if (admin.getRole() == Role.ROLE_ADMIN) {
                admin.setRole(Role.ROLE_SYSTEM_ADMIN);
                admin.setFullName("System Administrator");
                admin.setDepartment("Application Development");
                changed = true;
                log.info(">>> Existing account 'admin' is now System Administrator. Password unchanged.");
            }
            if (!admin.isEnabled()) {
                admin.setEnabled(true);
                changed = true;
                log.info(">>> System administrator account 'admin' was deactivated and is active again.");
            }
            if (changed) {
                userRepository.save(admin);
            } else {
                log.info(">>> System administrator account 'admin' already exists.");
            }
            return;
        }

        userRepository.save(newLocalAdmin(
                "admin",
                "admin@dashenbank.com",
                Role.ROLE_SYSTEM_ADMIN,
                "System Administrator",
                "Application Development"));
        log.info(">>> System administrator account 'admin' created.");
    }

    private void bootstrapOperationalAdministrator() {
        var operational = userRepository.findByUsernameIgnoreCase("opadmin");
        if (operational.isPresent()) {
            User opadmin = operational.get();
            if (!opadmin.isEnabled()) {
                opadmin.setEnabled(true);
                userRepository.save(opadmin);
                log.info(">>> Operational administrator account 'opadmin' was deactivated and is active again.");
            } else {
                log.info(">>> Operational administrator account 'opadmin' already exists.");
            }
            return;
        }

        userRepository.save(newLocalAdmin(
                "opadmin",
                "opadmin@dashenbank.com",
                Role.ROLE_ADMIN,
                "Operational Admin",
                "Operational Administration"));
        log.info(">>> Operational administrator account 'opadmin' created.");
    }

    private User newLocalAdmin(String username, String email, Role role, String fullName, String department) {
        return User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(resolveInitialAdminPassword()))
                .role(role)
                .authSource(AuthSource.LOCAL)
                .fullName(fullName)
                .department(department)
                .approved(true)
                .approvalStatus("APPROVED")
                .enabled(true)
                .mustChangePassword(true)
                .passwordChangedAt(java.time.LocalDateTime.now())
                .passwordExpiryDate(java.time.LocalDateTime.now().plusDays(
                        com.dashenbank.cms.security.SecurityPolicy.PASSWORD_EXPIRY_DAYS))
                .build();
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
