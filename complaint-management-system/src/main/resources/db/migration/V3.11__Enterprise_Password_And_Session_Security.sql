-- Account lockout, password expiry, password history, and security audit log.

ALTER TABLE `users`
    ADD COLUMN `failed_login_attempts` int NOT NULL DEFAULT 0,
    ADD COLUMN `account_locked` bit(1) NOT NULL DEFAULT b'0',
    ADD COLUMN `lockout_time` datetime(6) DEFAULT NULL,
    ADD COLUMN `password_changed_at` datetime(6) DEFAULT NULL,
    ADD COLUMN `password_expiry_date` datetime(6) DEFAULT NULL;

UPDATE `users`
SET `password_changed_at` = COALESCE(`created_at`, NOW(6))
WHERE `password_changed_at` IS NULL;

UPDATE `users`
SET `password_expiry_date` = DATE_ADD(`password_changed_at`, INTERVAL 90 DAY)
WHERE `password_expiry_date` IS NULL;

CREATE TABLE IF NOT EXISTS `password_history` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `user_id` bigint NOT NULL,
    `password_hash` varchar(255) NOT NULL,
    `created_at` datetime(6) NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_password_history_user` (`user_id`),
    CONSTRAINT `fk_password_history_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `security_audit_log` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `username` varchar(255) NOT NULL,
    `event_type` varchar(50) NOT NULL,
    `created_at` datetime(6) NOT NULL,
    `ip_address` varchar(64) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_security_audit_username` (`username`),
    KEY `idx_security_audit_created` (`created_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
