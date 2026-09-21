-- Remove unused SLA hop-escalation leftovers. Workflow status ESCALATED and
-- CRI NBE fields (escalated_to, date_of_escalation, reason_for_escalation)
-- are unchanged.
-- Columns on complaint_sla_metrics / sla_breach_records may be missing on
-- databases that never ran Hibernate ddl-auto=update, so drops are guarded.

DELETE FROM `sla_configs`
WHERE `config_key` IN (
    'ESCALATION_L1',
    'ESCALATION_L2',
    'ESCALATION_CM_MANAGER',
    'ESCALATION_DEPARTMENT_DIRECTOR'
)
   OR `config_group` = 'ESCALATION_SLA';

SET @metrics_escalation_level := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'complaint_sla_metrics'
      AND COLUMN_NAME = 'escalation_level'
);
SET @sql := IF(@metrics_escalation_level > 0,
    'ALTER TABLE `complaint_sla_metrics` DROP COLUMN `escalation_level`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @metrics_escalated_at := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'complaint_sla_metrics'
      AND COLUMN_NAME = 'escalated_at'
);
SET @sql := IF(@metrics_escalated_at > 0,
    'ALTER TABLE `complaint_sla_metrics` DROP COLUMN `escalated_at`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @breach_escalation_level := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sla_breach_records'
      AND COLUMN_NAME = 'escalation_level'
);
SET @sql := IF(@breach_escalation_level > 0,
    'ALTER TABLE `sla_breach_records` DROP COLUMN `escalation_level`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

DROP TABLE IF EXISTS `sla_escalation_records`;
