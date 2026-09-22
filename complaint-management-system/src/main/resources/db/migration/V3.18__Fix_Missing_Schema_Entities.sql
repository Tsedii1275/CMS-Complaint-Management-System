-- 1. Fix missing columns on existing tables
ALTER TABLE customers ADD COLUMN customer_sub_segment VARCHAR(50);
ALTER TABLE complaint_sla_metrics ADD COLUMN breach_reason VARCHAR(255);

-- 2. Create the missing districts table
CREATE TABLE districts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    region VARCHAR(255),
manager_name VARCHAR(255)
);
-- 3. Create the missing branches table
CREATE TABLE branches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    district_id BIGINT,
    code VARCHAR(50),
manager_name VARCHAR(255)
);
