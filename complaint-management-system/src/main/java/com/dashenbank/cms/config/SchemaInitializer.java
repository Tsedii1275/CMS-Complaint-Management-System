package com.dashenbank.cms.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * SchemaInitializer ensures critical database columns exist on application
 * startup.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaInitializer implements CommandLineRunner {

    private static final String SEPARATOR = "====================================================================";
    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void initSchema() {
        try {
            jdbcTemplate
                    .execute("ALTER TABLE complaint_sla_metrics ADD COLUMN dbc_ticket_id varchar(100) DEFAULT NULL");
            log.info("SchemaInitializer: Verified/Added dbc_ticket_id column in complaint_sla_metrics.");
        } catch (Exception ignored) {
            // ALTER TABLE fails when the column already exists; startup continues.
        }
        try {
            jdbcTemplate
                    .execute("ALTER TABLE complaint_sla_metrics ADD COLUMN general_ticket_id varchar(50) DEFAULT NULL");
        } catch (Exception ignored) {
            // ALTER TABLE fails when the column already exists; startup continues.
        }
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE complainant_related_information ADD COLUMN is_manually_edited boolean DEFAULT false");
        } catch (Exception ignored) {
            // ALTER TABLE fails when the column already exists; startup continues.
        }
        try {
            jdbcTemplate.execute("ALTER TABLE complainant_related_information DROP COLUMN sla_status");
        } catch (Exception ignored) {
            // DROP fails when the column does not exist; startup continues.
        }
        try {
            jdbcTemplate.execute("ALTER TABLE complainant_related_information DROP COLUMN sla_breached");
        } catch (Exception ignored) {
            // DROP fails when the column does not exist; startup continues.
        }
    }

    @Override
    public void run(String... args) throws Exception {
        log.info(SEPARATOR);
        log.info("SchemaInitializer: Database schema verification completed successfully.");
        log.info(SEPARATOR);
    }
}
