-- ============================================================================
-- Flyway V3.4 Narrow NBE Compliance Reports To Regulatory Fields Only
-- ============================================================================
-- Identity and operational complaint data must be loaded from
-- complainant_related_information and complaint_sla_metrics at report
-- generation time. This table keeps only NBE-specific fields, keyed by ticket
-- number. UNIQUE(ticket_number) is retained; the extra non-unique index is
-- redundant and is dropped. No foreign keys to Flowable tables.
-- ============================================================================

ALTER TABLE `nbe_compliance_reports`
    DROP COLUMN `complainant_name`,
    DROP COLUMN `mobile`,
    DROP COLUMN `email`,
    DROP COLUMN `issues_raised`,
    DROP COLUMN `days_open`;

ALTER TABLE `nbe_compliance_reports`
    DROP INDEX `idx_nbe_ticket_number`;
