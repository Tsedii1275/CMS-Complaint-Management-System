-- Align task_time_tracking with TaskTimeTracking JPA mappings.
-- Registering a complaint loads the first Flowable user task and Hibernate
-- SELECTs every entity column; missing assigned_* columns abort submit.

SET @db := DATABASE();

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'task_time_tracking' AND COLUMN_NAME = 'assigned_role') = 0,
        'ALTER TABLE `task_time_tracking` ADD COLUMN `assigned_role` varchar(50) DEFAULT NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'task_time_tracking' AND COLUMN_NAME = 'assigned_department') = 0,
        'ALTER TABLE `task_time_tracking` ADD COLUMN `assigned_department` varchar(100) DEFAULT NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'task_time_tracking' AND COLUMN_NAME = 'assigned_branch') = 0,
        'ALTER TABLE `task_time_tracking` ADD COLUMN `assigned_branch` varchar(100) DEFAULT NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'task_time_tracking' AND COLUMN_NAME = 'assigned_district') = 0,
        'ALTER TABLE `task_time_tracking` ADD COLUMN `assigned_district` varchar(100) DEFAULT NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'task_time_tracking' AND COLUMN_NAME = 'claimed_at') = 0,
        'ALTER TABLE `task_time_tracking` ADD COLUMN `claimed_at` datetime(6) DEFAULT NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'task_time_tracking' AND COLUMN_NAME = 'claimed_by') = 0,
        'ALTER TABLE `task_time_tracking` ADD COLUMN `claimed_by` varchar(100) DEFAULT NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'task_time_tracking' AND COLUMN_NAME = 'is_claimed') = 0,
        'ALTER TABLE `task_time_tracking` ADD COLUMN `is_claimed` bit(1) DEFAULT b''0''',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'task_time_tracking' AND COLUMN_NAME = 'response_time_minutes') = 0,
        'ALTER TABLE `task_time_tracking` ADD COLUMN `response_time_minutes` bigint DEFAULT NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'task_time_tracking' AND COLUMN_NAME = 'response_sla_target_minutes') = 0,
        'ALTER TABLE `task_time_tracking` ADD COLUMN `response_sla_target_minutes` int DEFAULT NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'task_time_tracking' AND COLUMN_NAME = 'response_sla_status') = 0,
        'ALTER TABLE `task_time_tracking` ADD COLUMN `response_sla_status` varchar(30) DEFAULT NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'task_time_tracking' AND COLUMN_NAME = 'response_breach_duration_minutes') = 0,
        'ALTER TABLE `task_time_tracking` ADD COLUMN `response_breach_duration_minutes` bigint DEFAULT NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'task_time_tracking' AND COLUMN_NAME = 'resolution_time_minutes') = 0,
        'ALTER TABLE `task_time_tracking` ADD COLUMN `resolution_time_minutes` bigint DEFAULT NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'task_time_tracking' AND COLUMN_NAME = 'resolution_sla_target_minutes') = 0,
        'ALTER TABLE `task_time_tracking` ADD COLUMN `resolution_sla_target_minutes` int DEFAULT NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'task_time_tracking' AND COLUMN_NAME = 'resolution_sla_status') = 0,
        'ALTER TABLE `task_time_tracking` ADD COLUMN `resolution_sla_status` varchar(30) DEFAULT NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'task_time_tracking' AND COLUMN_NAME = 'resolution_breach_duration_minutes') = 0,
        'ALTER TABLE `task_time_tracking` ADD COLUMN `resolution_breach_duration_minutes` bigint DEFAULT NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
