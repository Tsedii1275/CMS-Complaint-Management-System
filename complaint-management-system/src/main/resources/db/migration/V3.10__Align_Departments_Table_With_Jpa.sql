-- V1.0 created departments(department_id, department_name, description).
-- JPA Department maps id, name, manager_name, branch_id.
-- Hibernate ddl-auto=update added the JPA columns but left department_name
-- NOT NULL with no default, so DataSeeder inserts fail.

SET @db := DATABASE();

-- Drop unused users -> departments FK so the PK column can be renamed.
SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'users'
           AND CONSTRAINT_NAME = 'FK_users_department' AND CONSTRAINT_TYPE = 'FOREIGN KEY') > 0,
        'ALTER TABLE `users` DROP FOREIGN KEY `FK_users_department`',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'users' AND COLUMN_NAME = 'department_id') > 0,
        'ALTER TABLE `users` DROP COLUMN `department_id`',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- PK: department_id -> id
SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'departments' AND COLUMN_NAME = 'department_id') > 0
        AND (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'departments' AND COLUMN_NAME = 'id') = 0,
        'ALTER TABLE `departments` CHANGE COLUMN `department_id` `id` bigint NOT NULL AUTO_INCREMENT',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- JPA columns
SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'departments' AND COLUMN_NAME = 'name') = 0,
        'ALTER TABLE `departments` ADD COLUMN `name` varchar(255) NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'departments' AND COLUMN_NAME = 'manager_name') = 0,
        'ALTER TABLE `departments` ADD COLUMN `manager_name` varchar(255) DEFAULT NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'departments' AND COLUMN_NAME = 'branch_id') = 0,
        'ALTER TABLE `departments` ADD COLUMN `branch_id` bigint NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Copy legacy department_name into name, then drop the unused NOT NULL column.
SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'departments' AND COLUMN_NAME = 'department_name') > 0
        AND (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'departments' AND COLUMN_NAME = 'name') > 0,
        'UPDATE `departments` SET `name` = `department_name` WHERE (`name` IS NULL OR `name` = '''') AND IFNULL(`department_name`, '''') <> ''''',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'departments' AND COLUMN_NAME = 'department_name') > 0,
        'ALTER TABLE `departments` DROP COLUMN `department_name`',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'departments' AND COLUMN_NAME = 'description') > 0,
        'ALTER TABLE `departments` DROP COLUMN `description`',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
