-- ============================================================================
-- Flyway V3.23 Migration: Drop Obsolete Local Organizational Master Tables
-- ============================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- Drop obsolete local master tables safely without constraint name dependency
DROP TABLE IF EXISTS `departments`;

DROP TABLE IF EXISTS `branches`;

DROP TABLE IF EXISTS `districts`;

SET FOREIGN_KEY_CHECKS = 1;