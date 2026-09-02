-- Remove unused CAPA action fields (target date, priority, status).

ALTER TABLE `capa_actions`
    DROP COLUMN `target_date`,
    DROP COLUMN `priority`,
    DROP COLUMN `status`;
