-- ============================================================================
-- Retire the CMS-local simulated CBS customer master.
-- Authoritative customer profile is Oracle view RTVSCBS_CUST_PROFILE (live lookup).
-- ============================================================================
--
-- FK / reference inventory BEFORE DROP TABLE `customers`:
--
-- Flyway V1.0 live schema (UAT/local CMS):
--   customers.customer_id            PK only
--   complaints                       NO customer_id column, NO FK to customers
--                                    snapshots: customer_name, account_number,
--                                    preferred_contact_number, home_branch, district
--   V3.14 complaints snapshots       cif_number, core_banking_phone,
--                                    customer_home_branch, customer_district
--   complaint_sla_metrics            customer_name snapshot only
--   complainant_related_information  snapshot columns only
--   audit_logs / process variables   JSON/runtime snapshots, no FK
--
-- Stale cms_db.sql dump (not applied by Flyway):
--   FK_complaints_customer  complaints.customer_id -> customers.customer_id
--                           ON DELETE CASCADE
--   Drop that FK if present so complaint rows are not cascaded.
--
-- Candidate names customer_profile / customer_master / cbs_customer /
-- mock_customer / simulated_customer_profile are not present in Flyway.
--
-- Do NOT truncate complaint snapshot columns.

SET @db := DATABASE();

SET @sql := (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
         WHERE CONSTRAINT_SCHEMA = @db
           AND TABLE_NAME = 'complaints'
           AND CONSTRAINT_TYPE = 'FOREIGN KEY'
           AND CONSTRAINT_NAME = 'FK_complaints_customer') > 0,
        'ALTER TABLE `complaints` DROP FOREIGN KEY `FK_complaints_customer`',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

DROP TABLE IF EXISTS `customers`;
