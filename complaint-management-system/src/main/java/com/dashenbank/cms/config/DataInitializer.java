package com.dashenbank.cms.config;

import com.dashenbank.cms.model.Role;
import com.dashenbank.cms.model.User;
import com.dashenbank.cms.model.Customer;
import com.dashenbank.cms.repository.UserRepository;
import com.dashenbank.cms.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final String RETAIL_SEGMENT = "Retail";

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        try {
            jdbcTemplate.execute("ALTER TABLE users MODIFY COLUMN role VARCHAR(100) NOT NULL");
        } catch (Exception e) {
            log.info("Role column alter info: {}", e.getMessage());
        }

        int migrated = jdbcTemplate.update(
                """
                UPDATE users
                SET role = 'ROLE_CONTACT_CENTER_SENIOR_MANAGER'
                WHERE role IN (
                        'ROLE_CONTACT_CENTER_MANAGER',
                        'CONTACT_CENTER_MANAGER',
                        'Contact Center Manager'
                    )
                    OR REPLACE(UPPER(TRIM(role)), ' ', '_') IN (
                        'ROLE_CONTACT_CENTER_MANAGER',
                        'CONTACT_CENTER_MANAGER'
                    )
                """);
        if (migrated > 0) {
            log.info("Migrated {} user(s) from ROLE_CONTACT_CENTER_MANAGER to ROLE_CONTACT_CENTER_SENIOR_MANAGER.",
                    migrated);
        }

        // Operational users are managed strictly through the Admin Dashboard.
        // Seed Simulated Core Banking Customers
        seedCustomer("1234567890123", "CIF10001", "Abyssinia Corporates", "info@abyssinia.com", "+251912345678",
                "Corporate", "None", "LOW", true);
        seedCustomer("5555666677778", "CIF10002", "Abebe Bekele", "abebe.b@gmail.com", "+251911223344", RETAIL_SEGMENT,
                "Pensioner", "LOW", false);
        seedCustomer("8888999900000", "CIF10003", "Negash Welde", "negash.w@gmail.com", "+251915556677", RETAIL_SEGMENT,
                "Standard", "High Risk", false);
        seedCustomer("1111222233334", "CIF10004", "Tigist Girma", "tigist.g@gmail.com", "+251919998877", RETAIL_SEGMENT,
                "Standard", "LOW", false);

        // Core Users and Banking Customers initialized cleanly.
    }

    @SuppressWarnings("java:S107")
    private void seedUser(String username, String email, String password, Role role, String district, String branch, String department, String fullName) {
        User user = userRepository.findByUsernameIgnoreCase(username).orElse(null);
        if (user == null) {
            user = User.builder()
                    .username(username)
                    .email(email)
                    .role(role)
                    .enabled(true)
                    .district(district)
                    .branch(branch)
                    .department(department)
                    .fullName(fullName)
                    .build();
            log.info("Seeding new user: {}", username);
        } else {
            log.info("Updating existing user: {}", username);
            user.setDistrict(district);
            user.setBranch(branch);
            user.setDepartment(department);
            user.setFullName(fullName);
        }

        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
    }

    @SuppressWarnings("java:S107")
    private void seedCustomer(String accountNumber, String cifNumber, String name, String email, String phoneNumber,
            String segment, String subSegment, String riskRating, boolean isVip) {
        Customer customer = customerRepository.findByAccountNumber(accountNumber).orElse(null);
        if (customer == null) {
            customer = Customer.builder()
                    .accountNumber(accountNumber)
                    .cifNumber(cifNumber)
                    .name(name)
                    .email(email)
                    .phoneNumber(phoneNumber)
                    .customerSegment(segment)
                    .customerSubSegment(subSegment)
                    .riskRating(riskRating)
                    .isVip(isVip)
                    .customerType("RETAIL")
                    .customerSince(LocalDate.now(ZoneId.systemDefault()).minusYears(2))
                    .relationshipManager("Relationship Manager " + name)
                    .build();
            log.info("Seeding new simulated customer: {}", name);
            customerRepository.save(customer);
        }
    }
}
