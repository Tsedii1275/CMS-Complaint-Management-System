ALTER TABLE `customers`
    ADD COLUMN `home_branch` varchar(150) DEFAULT NULL AFTER `relationship_manager`,
    ADD COLUMN `district` varchar(100) DEFAULT NULL AFTER `home_branch`;

ALTER TABLE `complaints`
    ADD COLUMN `cif_number` varchar(20) DEFAULT NULL AFTER `account_number`,
    ADD COLUMN `core_banking_phone` varchar(20) DEFAULT NULL AFTER `preferred_contact_number`,
    ADD COLUMN `customer_home_branch` varchar(150) DEFAULT NULL AFTER `home_branch`,
    ADD COLUMN `customer_district` varchar(100) DEFAULT NULL AFTER `district`;
