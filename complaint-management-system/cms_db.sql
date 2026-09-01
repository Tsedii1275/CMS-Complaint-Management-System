-- ============================================================================
-- Complaint Management System (CMS) - Complete Database Schema Export
-- ============================================================================
-- Compatible with: MySQL 8+
-- Description: Complete SQL schema definitions matching the CMS data architecture.
--              Includes security tables, core complaint processing tables, SLA configurations,
--              attachments, notifications, and auditing structures.
-- Generated: 2026-07-11
-- ============================================================================

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";

START TRANSACTION;

SET time_zone = "+00:00";

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */
;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */
;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */
;
/*!40101 SET NAMES utf8mb4 */
;

--
-- Database: `cms_db`
--
CREATE DATABASE IF NOT EXISTS `cms_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

USE `cms_db`;

-- ============================================================================
-- 1. SECURITY & USER MANAGEMENT TABLES
-- ============================================================================

DROP TABLE IF EXISTS `roles`;

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
    `role` enum(
        'ROLE_ADMIN',
        'ROLE_AUDIT_TEAM',
        'ROLE_BRANCH_STAFF',
        'ROLE_CUSTOMER_CARE_OFFICER',
        'ROLE_DEPARTMENT_WORKUNIT',
        'ROLE_CHIEF_COMMITTEE'
    ) NOT NULL,
    `username` varchar(255) NOT NULL,
    `department_id` bigint DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UK_username` (`username`),
    KEY `FK_users_department` (`department_id`),
    CONSTRAINT `FK_users_department` FOREIGN KEY (`department_id`) REFERENCES `departments` (`department_id`) ON DELETE SET NULL
) ENGINE = InnoDB AUTO_INCREMENT = 8 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ============================================================================
-- 2. CUSTOMER & ACCOUNT COMPLIANCE TABLES
-- ============================================================================

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

-- ============================================================================
-- 3. COMPLAINT & WORKFLOW PATHWAY TABLES
-- ============================================================================

DROP TABLE IF EXISTS `complaint_categories`;

DROP TABLE IF EXISTS `complaint_priorities`;

CREATE TABLE IF NOT EXISTS `complaints` (
    `complaint_id` bigint NOT NULL AUTO_INCREMENT,
    `ticket_number` varchar(50) NOT NULL,
    `customer_id` bigint NOT NULL,
    `account_number` varchar(30) DEFAULT NULL,
    `account_status` varchar(20) DEFAULT NULL,
    `home_branch` varchar(100) DEFAULT NULL,
    `channel` varchar(30) NOT NULL DEFAULT 'web',
    `description` text NOT NULL,
    `category_name` varchar(100) DEFAULT NULL,
    `priority_level` varchar(50) DEFAULT 'GENERAL',
    `process_instance_id` varchar(100) DEFAULT NULL,
    `general_ticket_id` varchar(50) DEFAULT NULL,
    `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `resolved_at` timestamp NULL DEFAULT NULL,
    PRIMARY KEY (`complaint_id`),
    UNIQUE KEY `UK_ticket_number` (`ticket_number`),
    KEY `FK_complaints_customer` (`customer_id`),
    CONSTRAINT `FK_complaints_customer` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`customer_id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ============================================================================
-- 4. CASE LEVEL METRICS & TIMING TABLES
-- ============================================================================

DROP TABLE IF EXISTS `sla_configurations`;

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

-- ============================================================================
-- 5. WORKFLOW TRIAGE STAGE TRANSACTION TABLES
-- ============================================================================

DROP TABLE IF EXISTS `cmd_screenings`;

DROP TABLE IF EXISTS `investigations`;

DROP TABLE IF EXISTS `committee_reviews`;

DROP TABLE IF EXISTS `resolutions`;

DROP TABLE IF EXISTS `customer_feedbacks`;

CREATE TABLE IF NOT EXISTS `customer_feedback` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `ticket_id` varchar(100) NOT NULL,
    `process_instance_id` varchar(100) DEFAULT NULL,
    `satisfied` bit(1) NOT NULL,
    `comment` text DEFAULT NULL,
    `submitted_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
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

-- ============================================================================
-- 6. SUPPORTING MEDIA, NOTIFICATIONS & AUDITING
-- ============================================================================

CREATE TABLE IF NOT EXISTS `attachments` (
    `attachment_id` bigint NOT NULL AUTO_INCREMENT,
    `complaint_id` bigint NOT NULL,
    `uploader_id` bigint DEFAULT NULL,
    `file_name` varchar(255) NOT NULL,
    `file_url` varchar(500) NOT NULL,
    `file_type` varchar(100) NOT NULL,
    `file_size_bytes` bigint NOT NULL,
    `context_type` varchar(50) NOT NULL,
    `uploaded_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`attachment_id`),
    KEY `FK_attachment_complaint` (`complaint_id`),
    CONSTRAINT `FK_attachment_complaint` FOREIGN KEY (`complaint_id`) REFERENCES `complaints` (`complaint_id`) ON DELETE CASCADE
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
    PRIMARY KEY (`notification_id`),
    KEY `FK_notif_complaint` (`complaint_id`),
    CONSTRAINT `FK_notif_complaint` FOREIGN KEY (`complaint_id`) REFERENCES `complaints` (`complaint_id`) ON DELETE CASCADE
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
    `description` text,
    `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
    `complaint_category` varchar(100) DEFAULT NULL,
    `complaint_description` text,
    `customer_email` varchar(150) DEFAULT NULL,
    `customer_name` varchar(100) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB AUTO_INCREMENT = 38 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ============================================================================
-- SEED INITIAL DATA
-- ============================================================================

-- User provisioning is handled via Admin Dashboard and Bootstrap DataSeeder.

-- ============================================================================
-- 7. ROOT CAUSE ANALYSIS (RCA) MODULE TABLES
-- ============================================================================

CREATE TABLE IF NOT EXISTS `rca_cases` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `ticket_id` varchar(50) NOT NULL,
    `process_instance_id` varchar(100) DEFAULT NULL,
    `root_cause_category` varchar(100) DEFAULT NULL,
    `incident_date` timestamp NULL DEFAULT NULL,
    `rca_status` varchar(30) NOT NULL DEFAULT 'PENDING',
    `analysis_date` timestamp NULL DEFAULT NULL,
    `financial_impact` decimal(15, 2) DEFAULT '0.00',
    `reputational_risk` varchar(30) DEFAULT 'LOW',
    `compliance_impact` varchar(100) DEFAULT NULL,
    `operational_disruption` varchar(255) DEFAULT NULL,
    `risk_score` double DEFAULT '0.0',
    `preventive_strategy` text DEFAULT NULL,
    `rca_required` bit(1) DEFAULT NULL,
    `rca_summary` text DEFAULT NULL,
    `rca_owner` varchar(100) DEFAULT NULL,
    `rca_completion_date` timestamp NULL DEFAULT NULL,
    `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
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
    `root_cause_statement` text DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `FK_whys_rca` (`rca_case_id`),
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
    `verification_notes` text DEFAULT NULL,
    `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
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
    `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ============================================================================
-- 8. CUSTOMER FEEDBACK & SATISFACTION MEASUREMENT TABLES
-- ============================================================================

CREATE TABLE IF NOT EXISTS `customer_feedback` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `complaint_id` varchar(100) DEFAULT NULL,
    `ticket_number` varchar(100) NOT NULL,
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
    PRIMARY KEY (`id`),
    UNIQUE KEY `UK_feedback_token` (`secure_token`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

ALTER TABLE customer_feedback
MODIFY COLUMN satisfied bit(1) DEFAULT NULL;

ALTER TABLE customer_feedback
MODIFY COLUMN comment text DEFAULT NULL;

ALTER TABLE customer_feedback
MODIFY COLUMN ticket_id varchar(100) DEFAULT NULL;

ALTER TABLE customer_feedback
ADD COLUMN preferred_language varchar(30) DEFAULT 'english';

ALTER TABLE complaint_sla_metrics
ADD COLUMN department varchar(100) DEFAULT NULL;

ALTER TABLE complaint_sla_metrics
ADD COLUMN manager varchar(100) DEFAULT NULL;

ALTER TABLE complaint_sla_metrics
ADD COLUMN assigned_user_id bigint DEFAULT NULL;

ALTER TABLE users ADD COLUMN district varchar(100) DEFAULT NULL;

ALTER TABLE users ADD COLUMN branch varchar(100) DEFAULT NULL;

ALTER TABLE users ADD COLUMN department varchar(100) DEFAULT NULL;

ALTER TABLE users ADD COLUMN full_name varchar(255) DEFAULT NULL;

COMMIT;