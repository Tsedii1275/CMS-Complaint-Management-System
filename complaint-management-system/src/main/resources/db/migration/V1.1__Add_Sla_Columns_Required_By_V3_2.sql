-- Columns used by V3.2 that Hibernate added on long-lived databases but are
-- missing from a V1.0-only schema (fresh Docker). Safe if the column exists.

SET @db := DATABASE();

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'complaint_sla_metrics' AND COLUMN_NAME = 'dbc_ticket_id') = 0,
        'ALTER TABLE `complaint_sla_metrics` ADD COLUMN `dbc_ticket_id` varchar(100) DEFAULT NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'complaint_sla_metrics' AND COLUMN_NAME = 'current_stage') = 0,
        'ALTER TABLE `complaint_sla_metrics` ADD COLUMN `current_stage` varchar(100) DEFAULT NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
