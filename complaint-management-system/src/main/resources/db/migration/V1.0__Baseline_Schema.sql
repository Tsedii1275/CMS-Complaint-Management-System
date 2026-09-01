-- ============================================================================
-- Flyway V1.0 Baseline Schema Migration
-- ============================================================================

CREATE TABLE IF NOT EXISTS `departments` (
    `department_id` bigint NOT NULL AUTO_INCREMENT,
    `department_name` varchar(100) NOT NULL,
    `description` varchar(255) DEFAULT NULL,
    PRIMARY KEY (`department_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `users` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `created_at` datetime(6) DEFAULT NULL,
    `email` varchar(255) NOT NULL,
    `enabled` bit(1) NOT NULL,
    `password` varchar(255) NOT NULL,
    `role` varchar(100) NOT NULL,
    `username` varchar(255) NOT NULL,
    `department_id` bigint DEFAULT NULL,
    `district` varchar(100) DEFAULT NULL,
    `branch` varchar(100) DEFAULT NULL,
    `department` varchar(100) DEFAULT NULL,
    `full_name` varchar(255) DEFAULT NULL,
    `must_change_password` boolean DEFAULT FALSE,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UK_username` (`username`),
    KEY `FK_users_department` (`department_id`),
    CONSTRAINT `FK_users_department` FOREIGN KEY (`department_id`) REFERENCES `departments` (`department_id`) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `customers` (
    `customer_id` bigint NOT NULL AUTO_INCREMENT,
    `cif_number` varchar(20) NOT NULL,
    `account_number` varchar(30) NOT NULL,
    `name` varchar(255) NOT NULL,
    `email` varchar(255) DEFAULT NULL,
    `phone_number` varchar(20) DEFAULT NULL,
    `customer_type` varchar(50) NOT NULL DEFAULT 'RETAIL',
    `customer_segment` varchar(50) DEFAULT NULL,
    `is_vip` bit(1) NOT NULL DEFAULT b'0',
    `risk_rating` varchar(20) DEFAULT 'LOW',
    `customer_since` date DEFAULT NULL,
    `relationship_manager` varchar(255) DEFAULT NULL,
    PRIMARY KEY (`customer_id`),
    UNIQUE KEY `UK_cif_number` (`cif_number`),
    UNIQUE KEY `UK_account_number` (`account_number`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `complaints` (
    `complaint_id` bigint NOT NULL AUTO_INCREMENT,
    `ticket_number` varchar(100) DEFAULT NULL,
    `general_ticket_id` varchar(50) DEFAULT NULL,
    `customer_name` varchar(255) DEFAULT NULL,
    `preferred_contact_number` varchar(100) DEFAULT NULL,
    `preferred_contact_method` varchar(100) DEFAULT NULL,
    `home_branch` varchar(150) DEFAULT NULL,
    `district` varchar(100) DEFAULT NULL,
    `complaint_detail` text DEFAULT NULL,
    `complaint_category` varchar(150) DEFAULT NULL,
    `service_type` varchar(150) DEFAULT NULL,
    `complaint_made_on` varchar(100) DEFAULT NULL,
    `received_by` varchar(150) DEFAULT NULL,
    `account_number` varchar(100) DEFAULT NULL,
    `classification` varchar(50) DEFAULT 'COMPLAINT',
    `complaint_classification` varchar(50) DEFAULT 'General',
    `status` varchar(50) DEFAULT 'OPEN',
    PRIMARY KEY (`complaint_id`),
    UNIQUE KEY `uk_ticket_number` (`ticket_number`),
    UNIQUE KEY `uk_general_ticket_id` (`general_ticket_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `sla_configs` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `config_key` varchar(100) NOT NULL,
    `config_group` varchar(50) NOT NULL,
    `display_name` varchar(150) NOT NULL,
    `allowed_minutes` int NOT NULL,
    `priority` varchar(30) DEFAULT NULL,
    `department` varchar(100) DEFAULT NULL,
    `description` varchar(255) DEFAULT NULL,
    `updated_at` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UK_sla_config_key` (`config_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `complaint_sla_metrics` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `audit_duration` int DEFAULT NULL,
    `branch_staff_duration` int DEFAULT NULL,
    `breached` bit(1) DEFAULT NULL,
    `cmd_duration` int DEFAULT NULL,
    `complaint_category` varchar(100) DEFAULT NULL,
    `branch` varchar(100) DEFAULT NULL,
    `district` varchar(100) DEFAULT NULL,
    `channel` varchar(50) DEFAULT NULL,
    `fcr_status` bit(1) DEFAULT NULL,
    `customer_name` varchar(100) DEFAULT NULL,
    `status` varchar(30) DEFAULT NULL,
    `complaint_id` varchar(100) DEFAULT NULL,
    `general_ticket_id` varchar(50) DEFAULT NULL,
    `created_at` datetime(6) DEFAULT NULL,
    `deadline` datetime(6) DEFAULT NULL,
    `department_duration` int DEFAULT NULL,
    `process_instance_id` varchar(100) DEFAULT NULL,
    `remaining_minutes` int DEFAULT NULL,
    `resolved_at` datetime(6) DEFAULT NULL,
    `service_quality_duration` int DEFAULT NULL,
    `sla_status` varchar(30) DEFAULT NULL,
    `total_allowed_minutes` int DEFAULT NULL,
    `total_elapsed_minutes` int DEFAULT NULL,
    `complaint_classification` varchar(50) DEFAULT 'General',
    `classification` varchar(50) DEFAULT 'INTAKE',
    `department` varchar(100) DEFAULT NULL,
    `manager` varchar(100) DEFAULT NULL,
    `assigned_user_id` bigint DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UK_sla_proc_inst` (`process_instance_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `task_time_tracking` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `assigned_user` varchar(100) DEFAULT NULL,
    `complaint_id` varchar(100) DEFAULT NULL,
    `completed_at` datetime(6) DEFAULT NULL,
    `duration_hours` double DEFAULT NULL,
    `duration_minutes` bigint DEFAULT NULL,
    `lane_name` varchar(50) DEFAULT NULL,
    `process_instance_id` varchar(100) DEFAULT NULL,
    `started_at` datetime(6) DEFAULT NULL,
    `task_definition_key` varchar(100) DEFAULT NULL,
    `task_id` varchar(100) DEFAULT NULL,
    `task_name` varchar(255) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `customer_feedback` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `complaint_id` varchar(100) DEFAULT NULL,
    `ticket_id` varchar(100) DEFAULT NULL,
    `ticket_number` varchar(100) DEFAULT NULL,
    `satisfied` bit(1) DEFAULT NULL,
    `comment` text DEFAULT NULL,
    `resolution_confirmed` bit(1) DEFAULT NULL,
    `csat_score` int DEFAULT NULL,
    `nps_score` int DEFAULT NULL,
    `nps_comment` text,
    `ces_score` int DEFAULT NULL,
    `ces_comment` text,
    `additional_comments` text,
    `submitted_at` timestamp NULL DEFAULT NULL,
    `reopened_case` bit(1) DEFAULT b'0',
    `secure_token` varchar(100) DEFAULT NULL,
    `token_expired` bit(1) DEFAULT b'0',
    `feedback_request_sent_at` timestamp NULL DEFAULT NULL,
    `feedback_submitted_at` timestamp NULL DEFAULT NULL,
    `feedback_response_time` bigint DEFAULT NULL,
    `reopen_count` int DEFAULT '0',
    `customer_satisfaction_status` varchar(30) DEFAULT NULL,
    `preferred_language` varchar(30) DEFAULT 'english',
    PRIMARY KEY (`id`),
    UNIQUE KEY `UK_feedback_token` (`secure_token`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `first_contact_resolutions` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `complaint_id` varchar(100) DEFAULT NULL,
    `process_instance_id` varchar(100) DEFAULT NULL,
    `customer_name` varchar(255) DEFAULT NULL,
    `account_number` varchar(100) DEFAULT NULL,
    `complaint_category` varchar(100) DEFAULT NULL,
    `fcr_notes` text DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_fcr_complaint_id` (`complaint_id`),
    KEY `idx_fcr_process_inst` (`process_instance_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `attachments` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `complaint_id` varchar(100) DEFAULT NULL,
    `file_name` varchar(255) NOT NULL,
    `file_path` varchar(500) NOT NULL,
    `file_type` varchar(100) NOT NULL,
    `uploaded_by` varchar(100) DEFAULT NULL,
    `uploaded_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `notifications` (
    `notification_id` bigint NOT NULL AUTO_INCREMENT,
    `complaint_id` bigint NOT NULL,
    `recipient_contact` varchar(255) NOT NULL,
    `type` varchar(10) NOT NULL,
    `content` text NOT NULL,
    `sent_status` varchar(20) NOT NULL DEFAULT 'PENDING',
    `sent_at` timestamp NULL DEFAULT NULL,
    `failure_reason` varchar(255) DEFAULT NULL,
    PRIMARY KEY (`notification_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `audit_log` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `complaint_id` varchar(100) DEFAULT NULL,
    `general_ticket_id` varchar(50) DEFAULT NULL,
    `process_instance_id` varchar(100) DEFAULT NULL,
    `task_id` varchar(100) DEFAULT NULL,
    `action` varchar(100) DEFAULT NULL,
    `actor` varchar(100) DEFAULT NULL,
    `actor_id` varchar(100) DEFAULT NULL,
    `customer_name` varchar(100) DEFAULT NULL,
    `details_of_complaint` text DEFAULT NULL,
    `complaint_category` varchar(100) DEFAULT NULL,
    `description` text,
    `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `holiday_calendar` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `holiday_date` date NOT NULL,
    `holiday_name` varchar(150) NOT NULL,
    `holiday_type` varchar(50) DEFAULT 'PUBLIC_HOLIDAY',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_holiday_date` (`holiday_date`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;