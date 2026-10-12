-- Reclassify only the bootstrap username. Every other user-management account stays.
UPDATE users
SET role = 'ROLE_SYSTEM_ADMIN',
    full_name = 'System Administrator',
    department = 'Application Development'
WHERE username = 'admin'
  AND role = 'ROLE_ADMIN';
