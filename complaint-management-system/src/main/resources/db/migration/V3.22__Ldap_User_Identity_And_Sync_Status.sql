-- Hybrid AD identity on CMS users. Organizational columns are never filled from AD.
ALTER TABLE `users`
    ADD COLUMN `auth_source` varchar(20) NOT NULL DEFAULT 'LOCAL' AFTER `role`,
    ADD COLUMN `object_guid` varchar(64) DEFAULT NULL AFTER `auth_source`,
    ADD COLUMN `ad_job_title` varchar(255) DEFAULT NULL AFTER `object_guid`,
    ADD COLUMN `last_ldap_sync_at` datetime(6) DEFAULT NULL AFTER `ad_job_title`;

ALTER TABLE `users`
    ADD UNIQUE KEY `uk_users_object_guid` (`object_guid`);

CREATE TABLE IF NOT EXISTS `ldap_sync_status` (
    `id` bigint NOT NULL,
    `last_started_at` datetime(6) DEFAULT NULL,
    `last_finished_at` datetime(6) DEFAULT NULL,
    `last_success_at` datetime(6) DEFAULT NULL,
    `last_result` varchar(40) DEFAULT NULL,
    `users_synced` int NOT NULL DEFAULT 0,
    `users_failed` int NOT NULL DEFAULT 0,
    `last_error` varchar(500) DEFAULT NULL,
    `directory_reachable` bit(1) NOT NULL DEFAULT b'0',
    `last_health_check_at` datetime(6) DEFAULT NULL,
    `updated_at` datetime(6) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO `ldap_sync_status` (`id`, `last_result`, `users_synced`, `users_failed`, `directory_reachable`)
VALUES (1, 'NEVER_RUN', 0, 0, b'0');
