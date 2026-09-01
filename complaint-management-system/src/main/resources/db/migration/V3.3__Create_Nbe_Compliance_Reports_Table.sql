-- ============================================================================
-- Flyway V3.3 Create NBE Compliance Reports Persistence Table
-- ============================================================================
-- NBE Annex 1 / Annex 2 regulatory reporting values were previously edited into
-- Flowable runtime variables while the report grid was rebuilt from process
-- history and SLA metrics. Save and reload therefore used different sources and
-- edits were lost. This table is the durable store for NBE report edits and is
-- keyed by ticket number so it survives process completion.
-- ============================================================================

CREATE TABLE IF NOT EXISTS `nbe_compliance_reports` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `ticket_number` VARCHAR(100) NOT NULL UNIQUE,
    `process_instance_id` VARCHAR(64) NULL,
    `complainant_name` VARCHAR(255) NULL,
    `mobile` VARCHAR(100) NULL,
    `email` VARCHAR(255) NULL,
    `issues_raised` TEXT NULL,
    `report_status` VARCHAR(100) NULL,
    `days_open` INT NULL,
    `staff_handling` VARCHAR(255) NULL,
    `reason_for_non_resolution` TEXT NULL,
    `additional_comments` TEXT NULL,
    `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `updated_by` VARCHAR(100) NULL,
    INDEX `idx_nbe_ticket_number` (`ticket_number`),
    INDEX `idx_nbe_process_instance_id` (`process_instance_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
