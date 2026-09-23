package com.dashenbank.cms.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
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
    }
}
