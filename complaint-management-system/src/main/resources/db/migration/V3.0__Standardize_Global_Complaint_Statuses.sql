-- ============================================================================
-- Flyway V3.0 Standardize Global Complaint Statuses Migration
-- ============================================================================
-- Enforces six official complaint statuses across the system:
-- RECORDED, ONTRACK, ESCALATED, RESOLVED, CLOSED, DECLINED
-- ============================================================================

-- 1. Migrate legacy statuses in `complaints` table
UPDATE `complaints`
SET
    `status` = 'RECORDED'
WHERE
    UPPER(`status`) IN (
        'NEW',
        'SUBMITTED',
        'CREATED',
        'REGISTERED',
        'TICKET_GENERATED',
        'COMPLAINT_CREATED'
    );

UPDATE `complaints`
SET
    `status` = 'ONTRACK'
WHERE
    UPPER(`status`) IN (
        'IN_PROGRESS',
        'OPEN',
        'PENDING',
        'ASSIGNED',
        'UNDER_REVIEW',
        'ACTIVE',
        'ON_TRACK',
        'IN PROGRESS'
    );

UPDATE `complaints`
SET
    `status` = 'ESCALATED'
WHERE
    UPPER(`status`) IN (
        'UNDER_INVESTIGATION',
        'INVESTIGATING',
        'ESCALATED_FOR_INVESTIGATION',
        'COMMITTEE_REVIEW',
        'COMMITTEE_ACCEPTED',
        'COMMITTEE_REJECTED'
    );

UPDATE `complaints`
SET
    `status` = 'RESOLVED'
WHERE
    UPPER(`status`) IN (
        'RESOLUTION_PENDING',
        'RESOLUTION_GIVEN'
    );

UPDATE `complaints`
SET
    `status` = 'CLOSED'
WHERE
    UPPER(`status`) IN ('COMPLETED', 'CLOSED_PENDING');

UPDATE `complaints`
SET
    `status` = 'DECLINED'
WHERE
    UPPER(`status`) IN ('REJECTED');

-- Set default status to RECORDED for `complaints`
ALTER TABLE `complaints`
MODIFY COLUMN `status` varchar(50) NOT NULL DEFAULT 'RECORDED';

-- 2. Migrate legacy statuses in `complaint_sla_metrics` table
UPDATE `complaint_sla_metrics`
SET
    `status` = 'RECORDED'
WHERE
    UPPER(`status`) IN (
        'NEW',
        'SUBMITTED',
        'CREATED',
        'REGISTERED',
        'TICKET_GENERATED',
        'COMPLAINT_CREATED'
    );

UPDATE `complaint_sla_metrics`
SET
    `status` = 'ONTRACK'
WHERE
    UPPER(`status`) IN (
        'IN_PROGRESS',
        'OPEN',
        'PENDING',
        'ASSIGNED',
        'UNDER_REVIEW',
        'ACTIVE',
        'ON_TRACK',
        'OTHER',
        'IN PROGRESS'
    );

UPDATE `complaint_sla_metrics`
SET
    `status` = 'ESCALATED'
WHERE
    UPPER(`status`) IN (
        'UNDER_INVESTIGATION',
        'INVESTIGATING',
        'ESCALATED_FOR_INVESTIGATION',
        'COMMITTEE_REVIEW',
        'COMMITTEE_ACCEPTED',
        'COMMITTEE_REJECTED'
    );

UPDATE `complaint_sla_metrics`
SET
    `status` = 'RESOLVED'
WHERE
    UPPER(`status`) IN (
        'RESOLUTION_PENDING',
        'RESOLUTION_GIVEN'
    );

UPDATE `complaint_sla_metrics`
SET
    `status` = 'CLOSED'
WHERE
    UPPER(`status`) IN ('COMPLETED', 'CLOSED_PENDING');

UPDATE `complaint_sla_metrics`
SET
    `status` = 'DECLINED'
WHERE
    UPPER(`status`) IN ('REJECTED');

-- Set default status to RECORDED for `complaint_sla_metrics`
ALTER TABLE `complaint_sla_metrics`
MODIFY COLUMN `status` varchar(50) NOT NULL DEFAULT 'RECORDED';