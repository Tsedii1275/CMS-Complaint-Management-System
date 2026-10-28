-- Shared complaint categories. The original 11 stay as standard rows.
-- Staff can add more from registration. Operational admin can turn added rows off.

CREATE TABLE IF NOT EXISTS `complaint_categories` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(100) NOT NULL,
    `name_key` varchar(100) NOT NULL,
    `active` bit(1) NOT NULL DEFAULT b'1',
    `builtin` bit(1) NOT NULL DEFAULT b'0',
    `sort_order` int NOT NULL DEFAULT 100,
    `created_at` datetime(6) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_complaint_category_name_key` (`name_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO `complaint_categories` (`name`, `name_key`, `active`, `builtin`, `sort_order`, `created_at`)
SELECT 'Customer Service Issues', 'customer service issues', b'1', b'1', 1, CURRENT_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM `complaint_categories` WHERE `name_key` = 'customer service issues');

INSERT INTO `complaint_categories` (`name`, `name_key`, `active`, `builtin`, `sort_order`, `created_at`)
SELECT 'Transaction Error', 'transaction error', b'1', b'1', 2, CURRENT_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM `complaint_categories` WHERE `name_key` = 'transaction error');

INSERT INTO `complaint_categories` (`name`, `name_key`, `active`, `builtin`, `sort_order`, `created_at`)
SELECT 'Account Management', 'account management', b'1', b'1', 3, CURRENT_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM `complaint_categories` WHERE `name_key` = 'account management');

INSERT INTO `complaint_categories` (`name`, `name_key`, `active`, `builtin`, `sort_order`, `created_at`)
SELECT 'Banking App Issues', 'banking app issues', b'1', b'1', 4, CURRENT_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM `complaint_categories` WHERE `name_key` = 'banking app issues');

INSERT INTO `complaint_categories` (`name`, `name_key`, `active`, `builtin`, `sort_order`, `created_at`)
SELECT 'Credit/Financing Concerns', 'credit/financing concerns', b'1', b'1', 5, CURRENT_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM `complaint_categories` WHERE `name_key` = 'credit/financing concerns');

INSERT INTO `complaint_categories` (`name`, `name_key`, `active`, `builtin`, `sort_order`, `created_at`)
SELECT 'Fraud & Security Risk', 'fraud & security risk', b'1', b'1', 6, CURRENT_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM `complaint_categories` WHERE `name_key` = 'fraud & security risk');

INSERT INTO `complaint_categories` (`name`, `name_key`, `active`, `builtin`, `sort_order`, `created_at`)
SELECT 'Information Disclosure', 'information disclosure', b'1', b'1', 7, CURRENT_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM `complaint_categories` WHERE `name_key` = 'information disclosure');

INSERT INTO `complaint_categories` (`name`, `name_key`, `active`, `builtin`, `sort_order`, `created_at`)
SELECT 'ATM & Card Banking Issues', 'atm & card banking issues', b'1', b'1', 8, CURRENT_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM `complaint_categories` WHERE `name_key` = 'atm & card banking issues');

INSERT INTO `complaint_categories` (`name`, `name_key`, `active`, `builtin`, `sort_order`, `created_at`)
SELECT 'Policy & Compliance Disputes', 'policy & compliance disputes', b'1', b'1', 9, CURRENT_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM `complaint_categories` WHERE `name_key` = 'policy & compliance disputes');

INSERT INTO `complaint_categories` (`name`, `name_key`, `active`, `builtin`, `sort_order`, `created_at`)
SELECT 'System Failure', 'system failure', b'1', b'1', 10, CURRENT_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM `complaint_categories` WHERE `name_key` = 'system failure');

INSERT INTO `complaint_categories` (`name`, `name_key`, `active`, `builtin`, `sort_order`, `created_at`)
SELECT 'Branch Operation', 'branch operation', b'1', b'1', 11, CURRENT_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM `complaint_categories` WHERE `name_key` = 'branch operation');
