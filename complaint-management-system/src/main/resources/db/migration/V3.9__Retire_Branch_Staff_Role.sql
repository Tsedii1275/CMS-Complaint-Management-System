-- Retired ROLE_BRANCH_STAFF as a separate CMS identity.
-- Existing accounts map to Contact Center Agent (same intake task keys and /branch-staff workspace).
-- ROLE_CUSTOMER_EXPERIENCE_PARTNERSHIP is a new enum value; no user rows yet.

UPDATE `users`
SET `role` = 'ROLE_CONTACT_CENTER_AGENT'
WHERE `role` = 'ROLE_BRANCH_STAFF';
