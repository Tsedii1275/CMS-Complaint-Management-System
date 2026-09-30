-- Migration V3.24: Add User Access Approval Columns to users table

SET @db := DATABASE();

-- 1. Add approved column if missing
SET
    @sql := (
        SELECT IF(
                (
                    SELECT COUNT(*)
                    FROM information_schema.COLUMNS
                    WHERE
                        TABLE_SCHEMA = @db
                        AND TABLE_NAME = 'users'
                        AND COLUMN_NAME = 'approved'
                ) = 0, 'ALTER TABLE `users` ADD COLUMN `approved` BOOLEAN NOT NULL DEFAULT TRUE', 'SELECT 1'
            )
    );

PREPARE stmt FROM @sql;

EXECUTE stmt;

DEALLOCATE PREPARE stmt;

-- 2. Add approval_status column if missing
SET
    @sql := (
        SELECT IF(
                (
                    SELECT COUNT(*)
                    FROM information_schema.COLUMNS
                    WHERE
                        TABLE_SCHEMA = @db
                        AND TABLE_NAME = 'users'
                        AND COLUMN_NAME = 'approval_status'
                ) = 0, 'ALTER TABLE `users` ADD COLUMN `approval_status` VARCHAR(50) DEFAULT \'APPROVED\'', 'SELECT 1'
            )
    );

PREPARE stmt FROM @sql;

EXECUTE stmt;

DEALLOCATE PREPARE stmt;

-- 3. Update pending users
UPDATE users
SET
    approved = FALSE,
    approval_status = 'PENDING_APPROVAL'
WHERE
    role = 'ROLE_PENDING';

-- 4. Update existing active users
UPDATE users
SET
    approved = TRUE,
    approval_status = 'APPROVED'
WHERE (
        approved IS NULL
        OR approval_status IS NULL
    )
    AND role != 'ROLE_PENDING';