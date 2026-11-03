-- Amharic is Unicode. Free-text tables that were created with a narrower
-- charset cannot store it. New tables follow the database default.
-- Flowable id columns stay as they are: converting a whole ACT_* table
-- changes BYTEARRAY_ID_ and breaks the foreign key to ACT_GE_BYTEARRAY.

-- ALTER DATABASE cannot be prepared. PREPARE raises error 1295 and Flyway stops the app.
ALTER DATABASE CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @db := DATABASE();

-- Convert one existing table when it is present and not already utf8mb4.
-- Already-utf8mb4 tables are left untouched.

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.TABLES
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'notifications'
           AND TABLE_COLLATION NOT LIKE 'utf8mb4%') = 1,
        'ALTER TABLE `notifications` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.TABLES
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'audit_log'
           AND TABLE_COLLATION NOT LIKE 'utf8mb4%') = 1,
        'ALTER TABLE `audit_log` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.TABLES
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'security_audit_log'
           AND TABLE_COLLATION NOT LIKE 'utf8mb4%') = 1,
        'ALTER TABLE `security_audit_log` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.TABLES
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'customer_feedback'
           AND TABLE_COLLATION NOT LIKE 'utf8mb4%') = 1,
        'ALTER TABLE `customer_feedback` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.TABLES
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'complainant_related_information'
           AND TABLE_COLLATION NOT LIKE 'utf8mb4%') = 1,
        'ALTER TABLE `complainant_related_information` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.TABLES
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'customers'
           AND TABLE_COLLATION NOT LIKE 'utf8mb4%') = 1,
        'ALTER TABLE `customers` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Only the value columns. Leave id and foreign-key columns unchanged.
SET @sql := IFNULL((
    SELECT IF(CHARACTER_SET_NAME = 'utf8mb4', 'SELECT 1', CONCAT(
        'ALTER TABLE `ACT_RU_VARIABLE` MODIFY `TEXT_` ', COLUMN_TYPE,
        ' CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci',
        IF(IS_NULLABLE = 'NO', ' NOT NULL', ' NULL')))
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'ACT_RU_VARIABLE' AND COLUMN_NAME = 'TEXT_'
    LIMIT 1
), 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IFNULL((
    SELECT IF(CHARACTER_SET_NAME = 'utf8mb4', 'SELECT 1', CONCAT(
        'ALTER TABLE `ACT_RU_VARIABLE` MODIFY `TEXT2_` ', COLUMN_TYPE,
        ' CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci',
        IF(IS_NULLABLE = 'NO', ' NOT NULL', ' NULL')))
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'ACT_RU_VARIABLE' AND COLUMN_NAME = 'TEXT2_'
    LIMIT 1
), 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IFNULL((
    SELECT IF(CHARACTER_SET_NAME = 'utf8mb4', 'SELECT 1', CONCAT(
        'ALTER TABLE `ACT_HI_VARINST` MODIFY `TEXT_` ', COLUMN_TYPE,
        ' CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci',
        IF(IS_NULLABLE = 'NO', ' NOT NULL', ' NULL')))
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'ACT_HI_VARINST' AND COLUMN_NAME = 'TEXT_'
    LIMIT 1
), 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IFNULL((
    SELECT IF(CHARACTER_SET_NAME = 'utf8mb4', 'SELECT 1', CONCAT(
        'ALTER TABLE `ACT_HI_VARINST` MODIFY `TEXT2_` ', COLUMN_TYPE,
        ' CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci',
        IF(IS_NULLABLE = 'NO', ' NOT NULL', ' NULL')))
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'ACT_HI_VARINST' AND COLUMN_NAME = 'TEXT2_'
    LIMIT 1
), 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IFNULL((
    SELECT IF(CHARACTER_SET_NAME = 'utf8mb4', 'SELECT 1', CONCAT(
        'ALTER TABLE `ACT_HI_DETAIL` MODIFY `TEXT_` ', COLUMN_TYPE,
        ' CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci',
        IF(IS_NULLABLE = 'NO', ' NOT NULL', ' NULL')))
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'ACT_HI_DETAIL' AND COLUMN_NAME = 'TEXT_'
    LIMIT 1
), 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IFNULL((
    SELECT IF(CHARACTER_SET_NAME = 'utf8mb4', 'SELECT 1', CONCAT(
        'ALTER TABLE `ACT_HI_DETAIL` MODIFY `TEXT2_` ', COLUMN_TYPE,
        ' CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci',
        IF(IS_NULLABLE = 'NO', ' NOT NULL', ' NULL')))
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'ACT_HI_DETAIL' AND COLUMN_NAME = 'TEXT2_'
    LIMIT 1
), 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
