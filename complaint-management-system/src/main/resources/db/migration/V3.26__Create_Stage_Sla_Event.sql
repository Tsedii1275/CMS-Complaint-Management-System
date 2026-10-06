CREATE TABLE IF NOT EXISTS `stage_sla_event` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `complaint_id` VARCHAR(100) NULL,
    `process_instance_id` VARCHAR(100) NULL,
    `task_id` VARCHAR(100) NOT NULL,
    `task_definition_key` VARCHAR(100) NULL,
    `canonical_stage` VARCHAR(80) NOT NULL,
    `assigned_user` VARCHAR(100) NULL,
    `started_at` DATETIME(6) NULL,
    `completed_at` DATETIME(6) NULL,
    `allowed_minutes` INT NULL,
    `elapsed_business_minutes` INT NULL,
    `status` VARCHAR(30) NOT NULL DEFAULT 'IN_PROGRESS',
    `breached_at` DATETIME(6) NULL,
    `breach_recorded` TINYINT(1) NOT NULL DEFAULT 0,
    `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stage_sla_event_task_id` (`task_id`),
    KEY `idx_stage_sla_event_complaint` (`complaint_id`),
    KEY `idx_stage_sla_event_process` (`process_instance_id`),
    KEY `idx_stage_sla_event_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @db := DATABASE();

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'sla_breach_records' AND COLUMN_NAME = 'task_id') = 0,
        'ALTER TABLE `sla_breach_records` ADD COLUMN `task_id` VARCHAR(100) NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.STATISTICS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'sla_breach_records' AND INDEX_NAME = 'uk_sla_breach_task_id') = 0,
        'ALTER TABLE `sla_breach_records` ADD UNIQUE KEY `uk_sla_breach_task_id` (`task_id`)',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Historic backfill from Flowable when the history table exists.
SET @has_hi := (
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'ACT_HI_TASKINST'
);

SET @sql := IF(@has_hi > 0,
    'INSERT INTO stage_sla_event (
        complaint_id, process_instance_id, task_id, task_definition_key, canonical_stage,
        assigned_user, started_at, completed_at, allowed_minutes, elapsed_business_minutes,
        status, created_at, updated_at)
     SELECT
        COALESCE(m.complaint_id, m.dbc_ticket_id, h.PROC_INST_ID_),
        h.PROC_INST_ID_,
        h.ID_,
        h.TASK_DEF_KEY_,
        CASE
            WHEN h.TASK_DEF_KEY_ IN (''FormTask_12'',''FormTask_16'',''FormTask_24'',''FormTask_8'',''FormTask_10'',''FormTask_72'') THEN ''BRANCH_INTAKE''
            WHEN h.TASK_DEF_KEY_ = ''FormTask_20'' THEN ''CONTACT_CENTER_INTAKE''
            WHEN h.TASK_DEF_KEY_ = ''FormTask_67'' THEN ''CUSTOMER_NOTIFICATION''
            WHEN h.TASK_DEF_KEY_ = ''FormTask_43'' THEN ''CMD_SCREENING''
            WHEN h.TASK_DEF_KEY_ = ''FormTask_48'' THEN ''INVESTIGATION''
            WHEN h.TASK_DEF_KEY_ = ''FormTask_57'' THEN ''WORK_UNIT_RESOLUTION''
            WHEN h.TASK_DEF_KEY_ = ''ServiceTask_65'' THEN ''SERVICE_QUALITY_REVIEW''
            WHEN h.TASK_DEF_KEY_ = ''FormTask_ChiefCommittee'' THEN ''COMMITTEE_REVIEW''
            WHEN h.TASK_DEF_KEY_ = ''FormTask_CEX'' THEN ''CHIEF_EXPERIENCE_REVIEW''
            ELSE COALESCE(h.TASK_DEF_KEY_, ''UNKNOWN'')
        END,
        h.ASSIGNEE_,
        h.START_TIME_,
        h.END_TIME_,
        NULL,
        NULL,
        CASE
            WHEN h.END_TIME_ IS NULL THEN ''IN_PROGRESS''
            ELSE ''MET''
        END,
        NOW(6),
        NOW(6)
     FROM ACT_HI_TASKINST h
     LEFT JOIN complaint_sla_metrics m ON m.process_instance_id = h.PROC_INST_ID_
     WHERE h.ID_ IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM stage_sla_event e WHERE e.task_id = h.ID_)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
