-- Align customer_feedback with CustomerFeedback.
-- Opening a survey selects every mapped column. A missing column is shown
-- to the customer as "Survey Link Expired or Invalid".

SET @db := DATABASE();

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'customer_feedback' AND COLUMN_NAME = 'employee_id') = 0,
        'ALTER TABLE `customer_feedback` ADD COLUMN `employee_id` varchar(100) DEFAULT NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'customer_feedback' AND COLUMN_NAME = 'department') = 0,
        'ALTER TABLE `customer_feedback` ADD COLUMN `department` varchar(100) DEFAULT NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'customer_feedback' AND COLUMN_NAME = 'resolution_speed_rating') = 0,
        'ALTER TABLE `customer_feedback` ADD COLUMN `resolution_speed_rating` varchar(50) DEFAULT NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'customer_feedback' AND COLUMN_NAME = 'ease_rating') = 0,
        'ALTER TABLE `customer_feedback` ADD COLUMN `ease_rating` varchar(50) DEFAULT NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'customer_feedback' AND COLUMN_NAME = 'followup_status') = 0,
        'ALTER TABLE `customer_feedback` ADD COLUMN `followup_status` varchar(50) DEFAULT ''PENDING''',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'customer_feedback' AND COLUMN_NAME = 'followup_started_at') = 0,
        'ALTER TABLE `customer_feedback` ADD COLUMN `followup_started_at` datetime(6) DEFAULT NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'customer_feedback' AND COLUMN_NAME = 'followup_closed_at') = 0,
        'ALTER TABLE `customer_feedback` ADD COLUMN `followup_closed_at` datetime(6) DEFAULT NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'customer_feedback' AND COLUMN_NAME = 'followup_assigned_to') = 0,
        'ALTER TABLE `customer_feedback` ADD COLUMN `followup_assigned_to` varchar(100) DEFAULT NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
