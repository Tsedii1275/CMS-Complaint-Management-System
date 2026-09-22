-- Align complaint_sla_metrics (and related missing objects) with JPA.
-- V3.18 already ran on UAT without these columns; Flyway will not re-run it.
-- Each ADD is skipped when the column already exists.

SET @db := DATABASE();

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'customer_sub_segment') = 0,
        'ALTER TABLE `customers` ADD COLUMN `customer_sub_segment` varchar(50) DEFAULT NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'complaint_sla_metrics' AND COLUMN_NAME = 'breach_reason') = 0,
        'ALTER TABLE `complaint_sla_metrics` ADD COLUMN `breach_reason` varchar(255) DEFAULT NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'complaint_sla_metrics' AND COLUMN_NAME = 'breached_at') = 0,
        'ALTER TABLE `complaint_sla_metrics` ADD COLUMN `breached_at` datetime(6) DEFAULT NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'complaint_sla_metrics' AND COLUMN_NAME = 'requires_investigation') = 0,
        'ALTER TABLE `complaint_sla_metrics` ADD COLUMN `requires_investigation` bit(1) DEFAULT b''0''',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'complaint_sla_metrics' AND COLUMN_NAME = 'investigation_type') = 0,
        'ALTER TABLE `complaint_sla_metrics` ADD COLUMN `investigation_type` varchar(100) DEFAULT NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'complaint_sla_metrics' AND COLUMN_NAME = 'current_stage_started_at') = 0,
        'ALTER TABLE `complaint_sla_metrics` ADD COLUMN `current_stage_started_at` datetime(6) DEFAULT NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'complaint_sla_metrics' AND COLUMN_NAME = 'current_stage_due_time') = 0,
        'ALTER TABLE `complaint_sla_metrics` ADD COLUMN `current_stage_due_time` datetime(6) DEFAULT NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'complaint_sla_metrics' AND COLUMN_NAME = 'current_stage_allowed_minutes') = 0,
        'ALTER TABLE `complaint_sla_metrics` ADD COLUMN `current_stage_allowed_minutes` int DEFAULT NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'complaint_sla_metrics' AND COLUMN_NAME = 'current_stage_elapsed_minutes') = 0,
        'ALTER TABLE `complaint_sla_metrics` ADD COLUMN `current_stage_elapsed_minutes` int DEFAULT 0',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'complaint_sla_metrics' AND COLUMN_NAME = 'current_stage_status') = 0,
        'ALTER TABLE `complaint_sla_metrics` ADD COLUMN `current_stage_status` varchar(30) DEFAULT ''ON_TRACK''',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'complaint_sla_metrics' AND COLUMN_NAME = 'overall_sla_start_time') = 0,
        'ALTER TABLE `complaint_sla_metrics` ADD COLUMN `overall_sla_start_time` datetime(6) DEFAULT NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'complaint_sla_metrics' AND COLUMN_NAME = 'overall_sla_due_time') = 0,
        'ALTER TABLE `complaint_sla_metrics` ADD COLUMN `overall_sla_due_time` datetime(6) DEFAULT NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'complaint_sla_metrics' AND COLUMN_NAME = 'reminder_1_sent') = 0,
        'ALTER TABLE `complaint_sla_metrics` ADD COLUMN `reminder_1_sent` bit(1) DEFAULT b''0''',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'complaint_sla_metrics' AND COLUMN_NAME = 'reminder_2_sent') = 0,
        'ALTER TABLE `complaint_sla_metrics` ADD COLUMN `reminder_2_sent` bit(1) DEFAULT b''0''',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'complaint_sla_metrics' AND COLUMN_NAME = 'reminder_3_sent') = 0,
        'ALTER TABLE `complaint_sla_metrics` ADD COLUMN `reminder_3_sent` bit(1) DEFAULT b''0''',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `sla_breach_records` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `complaint_id` varchar(100) NOT NULL,
    `process_instance_id` varchar(100) DEFAULT NULL,
    `stage_name` varchar(100) DEFAULT NULL,
    `breach_reason` varchar(255) DEFAULT NULL,
    `breach_duration_minutes` bigint DEFAULT NULL,
    `responsible_work_unit` varchar(100) DEFAULT NULL,
    `escalation_actions_taken` varchar(500) DEFAULT NULL,
    `breach_timestamp` datetime(6) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `rca_cases` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `ticket_id` varchar(50) NOT NULL,
    `process_instance_id` varchar(100) DEFAULT NULL,
    `root_cause_category` varchar(100) DEFAULT NULL,
    `incident_date` datetime(6) DEFAULT NULL,
    `rca_status` varchar(30) NOT NULL DEFAULT 'PENDING',
    `analysis_date` datetime(6) DEFAULT NULL,
    `financial_impact` decimal(15, 2) DEFAULT '0.00',
    `reputational_risk` varchar(30) DEFAULT 'LOW',
    `compliance_impact` varchar(100) DEFAULT NULL,
    `operational_disruption` varchar(255) DEFAULT NULL,
    `risk_score` double DEFAULT '0.0',
    `preventive_strategy` text,
    `rca_required` bit(1) DEFAULT NULL,
    `rca_summary` text,
    `rca_owner` varchar(100) DEFAULT NULL,
    `rca_completion_date` datetime(6) DEFAULT NULL,
    `created_at` datetime(6) NOT NULL,
    `updated_at` datetime(6) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UK_rca_ticket` (`ticket_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `rca_5whys` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `rca_case_id` bigint NOT NULL,
    `why_1` varchar(255) DEFAULT NULL,
    `why_2` varchar(255) DEFAULT NULL,
    `why_3` varchar(255) DEFAULT NULL,
    `why_4` varchar(255) DEFAULT NULL,
    `why_5` varchar(255) DEFAULT NULL,
    `root_cause_statement` text,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rca_5whys_case` (`rca_case_id`),
    CONSTRAINT `FK_whys_rca` FOREIGN KEY (`rca_case_id`) REFERENCES `rca_cases` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `corrective_preventive_actions` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `rca_case_id` bigint NOT NULL,
    `action_type` varchar(20) NOT NULL,
    `action_description` text NOT NULL,
    `owner` varchar(100) NOT NULL,
    `target_date` date DEFAULT NULL,
    `implementation_status` varchar(30) NOT NULL DEFAULT 'PENDING',
    `effectiveness_rating` varchar(20) DEFAULT NULL,
    `verification_notes` text,
    `created_at` datetime(6) NOT NULL,
    PRIMARY KEY (`id`),
    KEY `FK_capa_rca` (`rca_case_id`),
    CONSTRAINT `FK_capa_rca` FOREIGN KEY (`rca_case_id`) REFERENCES `rca_cases` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `rca_audit_logs` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `rca_case_id` bigint DEFAULT NULL,
    `ticket_id` varchar(50) DEFAULT NULL,
    `action` varchar(100) NOT NULL,
    `actor` varchar(100) NOT NULL,
    `description` text,
    `created_at` datetime(6) NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
