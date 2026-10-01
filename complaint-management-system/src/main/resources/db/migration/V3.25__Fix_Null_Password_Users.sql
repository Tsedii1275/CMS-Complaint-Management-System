-- Repair any user records where password is NULL or empty to prevent JPA/SQL constraint errors
UPDATE users
SET password = '$2a$10$7R0wUq8Xw7N0u6S0P3K5.e8N0u6S0P3K5.e8N0u6S0P3K5.e8N0u'
WHERE
    password IS NULL
    OR password = '';