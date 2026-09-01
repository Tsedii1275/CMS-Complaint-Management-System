-- Retired ROLE_SERVICE_QUALITY (dedicated Service Quality officer workspace).
-- Existing accounts are mapped to Customer Care Officer so login still works.
-- ROLE_SERVICE_QUALITY_DIRECTOR is unchanged.

UPDATE `users`
SET `role` = 'ROLE_CUSTOMER_CARE_OFFICER'
WHERE `role` = 'ROLE_SERVICE_QUALITY';
