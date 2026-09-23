-- ============================================================================
-- Notification outbox + delivery audit.
-- Reuses the V1.0 `notifications` table (never written by application code)
-- instead of adding a parallel table. One row per customer notification per
-- channel; the dispatcher sends from it and records the real outcome.
--
-- V1.0 shape: notification_id, complaint_id BIGINT NOT NULL, recipient_contact,
--             type, content, sent_status, sent_at TIMESTAMP, failure_reason(255)
-- complaint_id becomes VARCHAR because tickets are DBC-/CM- strings.
-- ============================================================================

SET @db := DATABASE();

CREATE TABLE IF NOT EXISTS `notifications` (
    `notification_id` bigint NOT NULL AUTO_INCREMENT,
    `complaint_id` varchar(100) DEFAULT NULL,
    `recipient_contact` varchar(255) NOT NULL,
    `type` varchar(10) NOT NULL,
    `content` text NOT NULL,
    `sent_status` varchar(20) NOT NULL DEFAULT 'PENDING',
    `sent_at` datetime(6) DEFAULT NULL,
    `failure_reason` varchar(500) DEFAULT NULL,
    PRIMARY KEY (`notification_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

ALTER TABLE `notifications`
    MODIFY COLUMN `complaint_id` varchar(100) DEFAULT NULL,
    MODIFY COLUMN `sent_at` datetime(6) DEFAULT NULL,
    MODIFY COLUMN `failure_reason` varchar(500) DEFAULT NULL;

SET @sql := (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'notifications' AND COLUMN_NAME = 'process_instance_id') = 0,
    'ALTER TABLE `notifications` ADD COLUMN `process_instance_id` varchar(100) DEFAULT NULL AFTER `complaint_id`',
    'SELECT 1'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'notifications' AND COLUMN_NAME = 'event_type') = 0,
    'ALTER TABLE `notifications` ADD COLUMN `event_type` varchar(50) NOT NULL DEFAULT ''UNKNOWN'' AFTER `process_instance_id`',
    'SELECT 1'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'notifications' AND COLUMN_NAME = 'provider') = 0,
    'ALTER TABLE `notifications` ADD COLUMN `provider` varchar(50) DEFAULT NULL AFTER `type`',
    'SELECT 1'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'notifications' AND COLUMN_NAME = 'language') = 0,
    'ALTER TABLE `notifications` ADD COLUMN `language` varchar(10) DEFAULT NULL AFTER `provider`',
    'SELECT 1'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'notifications' AND COLUMN_NAME = 'subject') = 0,
    'ALTER TABLE `notifications` ADD COLUMN `subject` varchar(255) DEFAULT NULL AFTER `recipient_contact`',
    'SELECT 1'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'notifications' AND COLUMN_NAME = 'attempts') = 0,
    'ALTER TABLE `notifications` ADD COLUMN `attempts` int NOT NULL DEFAULT 0 AFTER `sent_status`',
    'SELECT 1'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'notifications' AND COLUMN_NAME = 'max_attempts') = 0,
    'ALTER TABLE `notifications` ADD COLUMN `max_attempts` int NOT NULL DEFAULT 5 AFTER `attempts`',
    'SELECT 1'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'notifications' AND COLUMN_NAME = 'next_attempt_at') = 0,
    'ALTER TABLE `notifications` ADD COLUMN `next_attempt_at` datetime(6) DEFAULT NULL AFTER `max_attempts`',
    'SELECT 1'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'notifications' AND COLUMN_NAME = 'last_attempt_at') = 0,
    'ALTER TABLE `notifications` ADD COLUMN `last_attempt_at` datetime(6) DEFAULT NULL AFTER `next_attempt_at`',
    'SELECT 1'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'notifications' AND COLUMN_NAME = 'provider_message_id') = 0,
    'ALTER TABLE `notifications` ADD COLUMN `provider_message_id` varchar(255) DEFAULT NULL AFTER `failure_reason`',
    'SELECT 1'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'notifications' AND COLUMN_NAME = 'idempotency_key') = 0,
    'ALTER TABLE `notifications` ADD COLUMN `idempotency_key` varchar(191) DEFAULT NULL AFTER `provider_message_id`',
    'SELECT 1'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'notifications' AND COLUMN_NAME = 'created_at') = 0,
    'ALTER TABLE `notifications` ADD COLUMN `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)',
    'SELECT 1'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'notifications' AND COLUMN_NAME = 'updated_at') = 0,
    'ALTER TABLE `notifications` ADD COLUMN `updated_at` datetime(6) DEFAULT NULL',
    'SELECT 1'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Dispatcher poll: due PENDING / RETRY rows.
SET @sql := (SELECT IF((SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'notifications' AND INDEX_NAME = 'idx_notifications_due') = 0,
    'CREATE INDEX `idx_notifications_due` ON `notifications` (`sent_status`, `next_attempt_at`)',
    'SELECT 1'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (SELECT IF((SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'notifications' AND INDEX_NAME = 'idx_notifications_complaint') = 0,
    'CREATE INDEX `idx_notifications_complaint` ON `notifications` (`complaint_id`)',
    'SELECT 1'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Not UNIQUE: a duplicate-key error inside a Flowable transaction would roll
-- back the complaint itself. Duplicates are prevented by an existence check.
SET @sql := (SELECT IF((SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'notifications' AND INDEX_NAME = 'idx_notifications_idempotency') = 0,
    'CREATE INDEX `idx_notifications_idempotency` ON `notifications` (`idempotency_key`)',
    'SELECT 1'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
