-- Align stored overall complaint status with:
-- RECORDED, ON_TRACK, ESCALATED, RESOLVED, CLOSED, DECLINED
-- RESOLVED = Customer Care Officer closed the case
-- CLOSED = customer submitted post-resolution feedback
--
-- String compares use an explicit collation so tables created with
-- utf8mb4_unicode_ci and utf8mb4_0900_ai_ci can still be joined.

UPDATE `complaint_sla_metrics`
SET `status` = 'ON_TRACK'
WHERE UPPER(`status`) IN ('ONTRACK', 'ON TRACK', 'IN_PROGRESS', 'IN PROGRESS', 'PENDING', 'ASSIGNED', 'UNDER_REVIEW');

UPDATE `complaints`
SET `status` = 'ON_TRACK'
WHERE UPPER(`status`) IN ('ONTRACK', 'ON TRACK', 'IN_PROGRESS', 'IN PROGRESS', 'PENDING', 'ASSIGNED', 'UNDER_REVIEW');

UPDATE `complainant_related_information`
SET `case_status` = 'ON_TRACK'
WHERE UPPER(`case_status`) IN ('ONTRACK', 'ON TRACK', 'IN_PROGRESS', 'IN PROGRESS', 'PENDING', 'ASSIGNED', 'UNDER_REVIEW');

UPDATE `complaint_sla_metrics` m
SET m.`status` = 'RESOLVED'
WHERE UPPER(m.`status`) = 'CLOSED'
  AND NOT EXISTS (
    SELECT 1 FROM `customer_feedback` f
    WHERE f.feedback_submitted_at IS NOT NULL
      AND (
        f.ticket_number COLLATE utf8mb4_unicode_ci = m.complaint_id COLLATE utf8mb4_unicode_ci
        OR f.ticket_number COLLATE utf8mb4_unicode_ci = m.general_ticket_id COLLATE utf8mb4_unicode_ci
        OR f.ticket_number COLLATE utf8mb4_unicode_ci = m.dbc_ticket_id COLLATE utf8mb4_unicode_ci
      )
  );

UPDATE `complaints` c
SET c.`status` = 'RESOLVED'
WHERE UPPER(c.`status`) = 'CLOSED'
  AND NOT EXISTS (
    SELECT 1 FROM `customer_feedback` f
    WHERE f.feedback_submitted_at IS NOT NULL
      AND (
        f.ticket_number COLLATE utf8mb4_unicode_ci = c.ticket_number COLLATE utf8mb4_unicode_ci
        OR f.ticket_number COLLATE utf8mb4_unicode_ci = c.general_ticket_id COLLATE utf8mb4_unicode_ci
      )
  );

UPDATE `complainant_related_information` cri
SET cri.`case_status` = 'RESOLVED'
WHERE UPPER(cri.`case_status`) = 'CLOSED'
  AND NOT EXISTS (
    SELECT 1 FROM `customer_feedback` f
    WHERE f.feedback_submitted_at IS NOT NULL
      AND f.ticket_number COLLATE utf8mb4_unicode_ci = cri.unique_id_no COLLATE utf8mb4_unicode_ci
  );

UPDATE `complaint_sla_metrics`
SET `status` = 'ESCALATED'
WHERE (UPPER(`current_stage`) LIKE '%COMMITTEE%' OR UPPER(`current_stage`) LIKE '%INVESTIG%' OR UPPER(`current_stage`) LIKE '%AUDIT%' OR UPPER(`current_stage`) LIKE '%CHIEF%')
  AND UPPER(COALESCE(`status`,'')) NOT IN ('RESOLVED', 'CLOSED', 'DECLINED');
