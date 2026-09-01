-- ============================================================================
-- Flyway V2.0 Controlled Deprecation & Schema Standardization Migration
-- ============================================================================

-- Controlled cleanup of verified redundant legacy tables
DROP TABLE IF EXISTS `cmd_screenings`;

DROP TABLE IF EXISTS `committee_reviews`;

DROP TABLE IF EXISTS `complaint_categories`;

DROP TABLE IF EXISTS `complaint_priorities`;

DROP TABLE IF EXISTS `investigations`;

DROP TABLE IF EXISTS `resolutions`;

DROP TABLE IF EXISTS `roles`;

DROP TABLE IF EXISTS `sla_configurations`;

DROP TABLE IF EXISTS `customer_feedbacks`;