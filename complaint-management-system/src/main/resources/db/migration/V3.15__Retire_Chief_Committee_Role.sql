-- Retired ROLE_CHIEF_COMMITTEE as a separate CMS identity.
-- Existing accounts map to Committee Secretary (same /chief-committee workspace).
-- Committee review tasks (FormTask_ChiefCommittee) are unchanged.

UPDATE `users`
SET `role` = 'ROLE_COMMITTEE_SECRETARY'
WHERE `role` = 'ROLE_CHIEF_COMMITTEE';
