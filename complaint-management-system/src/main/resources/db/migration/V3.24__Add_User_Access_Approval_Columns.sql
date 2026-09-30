-- Migration V3.24: Add User Access Approval Columns to users table

ALTER TABLE users
ADD COLUMN IF NOT EXISTS approved BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE users
ADD COLUMN IF NOT EXISTS approval_status VARCHAR(50) DEFAULT 'APPROVED';

-- Update users having ROLE_PENDING to unapproved status
UPDATE users
SET
    approved = FALSE,
    approval_status = 'PENDING_APPROVAL'
WHERE
    role = 'ROLE_PENDING';

-- Ensure all existing active users are marked as APPROVED
UPDATE users
SET
    approved = TRUE,
    approval_status = 'APPROVED'
WHERE (
        approved IS NULL
        OR approval_status IS NULL
    )
    AND role != 'ROLE_PENDING';