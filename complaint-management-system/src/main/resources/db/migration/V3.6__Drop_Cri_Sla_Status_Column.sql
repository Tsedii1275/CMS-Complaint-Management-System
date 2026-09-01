-- Remove SLA status from Complainant Related Information if it was added
-- outside the original CRI schema (for example by Hibernate or a manual ALTER).
-- complaint_sla_metrics.sla_status is unchanged.

SET @cri_sla_status := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'complainant_related_information'
      AND COLUMN_NAME = 'sla_status'
);
SET @sql := IF(@cri_sla_status > 0,
    'ALTER TABLE `complainant_related_information` DROP COLUMN `sla_status`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @cri_sla_breached := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'complainant_related_information'
      AND COLUMN_NAME = 'sla_breached'
);
SET @sql := IF(@cri_sla_breached > 0,
    'ALTER TABLE `complainant_related_information` DROP COLUMN `sla_breached`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
