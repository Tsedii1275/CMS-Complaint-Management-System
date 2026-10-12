-- ROLE_ADMIN stays the operational administrator. Existing accounts are not
-- promoted to ROLE_SYSTEM_ADMIN. Only the bootstrap display name is clarified.
UPDATE users
SET full_name = 'Operational Admin',
    department = 'Operational Administration'
WHERE username = 'admin'
  AND role = 'ROLE_ADMIN'
  AND full_name = 'System Administrator';
