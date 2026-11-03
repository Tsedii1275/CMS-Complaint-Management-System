-- Amharic is Unicode. Free-text tables that were created with a narrower
-- charset cannot store it. New tables, including Flowable variable tables
-- created after this runs, follow the database default.

SET @db := DATABASE();
SET @alterDb := CONCAT(
    'ALTER DATABASE `', REPLACE(@db, '`', '``'),
    '` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci'
);
PREPARE stmt FROM @alterDb;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

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

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.TABLES
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'ACT_RU_VARIABLE'
           AND TABLE_COLLATION NOT LIKE 'utf8mb4%') = 1,
        'ALTER TABLE `ACT_RU_VARIABLE` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.TABLES
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'ACT_HI_VARINST'
           AND TABLE_COLLATION NOT LIKE 'utf8mb4%') = 1,
        'ALTER TABLE `ACT_HI_VARINST` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.TABLES
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'ACT_HI_DETAIL'
           AND TABLE_COLLATION NOT LIKE 'utf8mb4%') = 1,
        'ALTER TABLE `ACT_HI_DETAIL` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
