package com.dashenbank.cms.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flyway configuration strategy that automatically executes flyway.repair()
 * prior to flyway.migrate() on application startup.
 * This automatically clears failed schema history entries and updates
 * checksums.
 */
@Configuration
public class FlywayConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayConfig.class);

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            log.info("Executing Flyway repair to clean failed migrations and update checksums...");
            flyway.repair();
            log.info("Executing Flyway migration...");
            flyway.migrate();
        };
    }
}
