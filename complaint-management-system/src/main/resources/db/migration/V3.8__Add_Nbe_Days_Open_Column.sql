-- Restore editable NBE Annex "No. of Days Issue Takes" on the compliance report row.
ALTER TABLE `nbe_compliance_reports`
    ADD COLUMN `days_open` INT NULL AFTER `staff_handling`;
