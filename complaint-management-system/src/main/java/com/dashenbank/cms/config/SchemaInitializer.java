package com.dashenbank.cms.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Schema changes belong in Flyway migrations under {@code db/migration}.
 * This runner only records that Flyway is the schema authority.
 */
@Slf4j
@Component
public class SchemaInitializer implements CommandLineRunner {

    private static final String SEPARATOR = "====================================================================";

    @Override
    public void run(String... args) {
        log.info(SEPARATOR);
        log.info("SchemaInitializer: schema is managed exclusively by Flyway. No HTTP or startup ALTER statements.");
        log.info(SEPARATOR);
    }
}
