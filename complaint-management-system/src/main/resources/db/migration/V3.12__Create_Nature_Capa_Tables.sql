-- CAPA analysis keyed by Nature of Complaint (RCA dashboard).
-- Does not duplicate complaint rows; nature is the grouping key into CRI.

CREATE TABLE IF NOT EXISTS `capa_analysis` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `complaint_nature` varchar(255) NOT NULL,
    `problem_statement` text,
    `root_cause` text,
    `updated_by` varchar(100) DEFAULT NULL,
    `created_at` datetime(6) NOT NULL,
    `updated_at` datetime(6) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_capa_analysis_nature` (`complaint_nature`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `capa_five_whys` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `capa_analysis_id` bigint NOT NULL,
    `why_1` text,
    `why_2` text,
    `why_3` text,
    `why_4` text,
    `why_5` text,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_capa_five_whys_analysis` (`capa_analysis_id`),
    CONSTRAINT `fk_capa_five_whys_analysis` FOREIGN KEY (`capa_analysis_id`) REFERENCES `capa_analysis` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `capa_actions` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `capa_analysis_id` bigint NOT NULL,
    `action_type` varchar(20) NOT NULL,
    `action_text` text,
    `responsible_department` varchar(255) DEFAULT NULL,
    `responsible_officer` varchar(255) DEFAULT NULL,
    `target_date` date DEFAULT NULL,
    `priority` varchar(30) DEFAULT NULL,
    `status` varchar(30) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_capa_actions_type` (`capa_analysis_id`, `action_type`),
    CONSTRAINT `fk_capa_actions_analysis` FOREIGN KEY (`capa_analysis_id`) REFERENCES `capa_analysis` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
