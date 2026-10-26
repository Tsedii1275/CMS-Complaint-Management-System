// Audit Investigation workflow (Requires Investigation):
//   CCO -> Customer Care Senior Manager -> Service Quality -> Chief Experience Officer
//       -> Audit Investigation Team   (Send to Audit Investigation)
//       -> Senior Manager closure     (Return to Customer Care Senior Manager)

export const TASK_SM_REVIEW = 'FormTask_SM_Review';
export const TASK_SQ_REVIEW = 'FormTask_SQ_Review';
export const TASK_SM_CLOSURE = 'FormTask_SM_Closure';
export const TASK_CXO = 'FormTask_CEX';

export const CXO_SEND_TO_AUDIT = 'SEND_TO_AUDIT';
export const CXO_RETURN_TO_SENIOR_MANAGER = 'RETURN_TO_SENIOR_MANAGER';

export const INVESTIGATION_STEPS = {
  [TASK_SM_REVIEW]: {
    label: 'Senior Manager Review',
    modalTitle: 'Investigation Review - Customer Care Senior Manager',
    actionLabel: 'Forward to Service Quality',
    successText: 'Case reviewed and forwarded to the Service Quality Department.',
    remarksLabel: 'Senior Manager Remarks',
    remarksPlaceholder: 'Enter your review remarks for the Service Quality Department...',
    remarksRequired: false,
    nextOwner: 'Service Quality Department'
  },
  [TASK_SQ_REVIEW]: {
    label: 'Service Quality Review',
    modalTitle: 'Investigation Review - Service Quality Department',
    actionLabel: 'Forward to Chief Experience Officer',
    successText: 'Case reviewed and forwarded to the Chief Experience Officer.',
    remarksLabel: 'Service Quality Remarks',
    remarksPlaceholder: 'Enter your review remarks for the Chief Experience Officer...',
    remarksRequired: false,
    nextOwner: 'Chief Experience Officer'
  },
  [TASK_SM_CLOSURE]: {
    label: 'Closure (No Audit)',
    modalTitle: 'Close Case Without Audit Investigation',
    actionLabel: 'Close Case & Notify Customer',
    successText: 'Case closed without audit investigation and the customer has been notified.',
    remarksLabel: 'Closure Summary (shared with the customer)',
    remarksPlaceholder: 'Summarise the outcome that will be sent to the customer...',
    remarksRequired: true,
    nextOwner: 'Customer'
  }
};

export const TRAIL_ACTION_LABELS = {
  REQUIRES_INVESTIGATION_SELECTED: 'Marked as Requires Investigation by Customer Care Officer',
  FORWARDED_TO_SERVICE_QUALITY: 'Senior Manager forwarded to Service Quality',
  FORWARDED_TO_CHIEF_EXPERIENCE_OFFICER: 'Service Quality forwarded to Chief Experience Officer',
  SENT_TO_AUDIT_INVESTIGATION: 'Chief Experience Officer sent to Audit Investigation',
  RETURNED_TO_SENIOR_MANAGER: 'Chief Experience Officer returned to Senior Manager',
  CLOSED_WITHOUT_AUDIT: 'Senior Manager closed the case without audit'
};

export function isInvestigationReviewTask(task) {
  return Boolean(task && INVESTIGATION_STEPS[task.definitionKey]);
}

export function investigationTaskKeysForRole(role) {
  switch (role) {
    case 'ROLE_CUSTOMER_CARE_SENIOR_MANAGER':
      return [TASK_SM_REVIEW, TASK_SM_CLOSURE];
    case 'ROLE_SERVICE_QUALITY_DIRECTOR':
      return [TASK_SQ_REVIEW];
    case 'ROLE_ADMIN':
      return [TASK_SM_REVIEW, TASK_SQ_REVIEW, TASK_SM_CLOSURE];
    default:
      return [];
  }
}

export function investigationTasksForRole(tasks, role) {
  const keys = investigationTaskKeysForRole(role);
  if (keys.length === 0) {
    return [];
  }
  return (tasks || []).filter((task) => keys.includes(task?.definitionKey));
}

export function hasInvestigationTab(role) {
  return investigationTaskKeysForRole(role).length > 0;
}

export function investigationTabLabel(role) {
  return role === 'ROLE_SERVICE_QUALITY_DIRECTOR' ? 'Service Quality Review' : 'Investigation Review';
}

export function reviewTrailOf(task) {
  const trail = task?.variables?.investigationReviewTrail;
  return Array.isArray(trail) ? trail : [];
}

/** Payload for Senior Manager / Service Quality / closure steps. */
export function buildReviewPayload(remarks, files) {
  return {
    reviewRemarks: (remarks || '').trim(),
    reviewFiles: files || []
  };
}

/** Payload for the Chief Experience Officer decision. */
export function buildCxoDecisionPayload(decision, { remarks, reason, files, existingFiles = [] }) {
  if (decision === CXO_RETURN_TO_SENIOR_MANAGER) {
    return {
      requiresInvestigation: true,
      cxoDecision: CXO_RETURN_TO_SENIOR_MANAGER,
      cxoReturnReason: (reason || '').trim(),
      reviewFiles: files || []
    };
  }
  return {
    requiresInvestigation: true,
    cxoDecision: CXO_SEND_TO_AUDIT,
    currentStage: 'CHIEF_OPERATION_AUDIT',
    stage: 'AUDIT_INVESTIGATION',
    escalatedBy: 'Chief Experience Officer',
    cxRemarks: (remarks || '').trim() || 'Escalated to Chief Operation Audit for operational investigation.',
    cxFiles: files || [],
    investigationFiles: [...existingFiles, ...(files || [])],
    nextStage: 'COMMITTEE_REVIEW',
    auditAccessRoles: ['ROLE_AUDIT_INVESTIGATION_TEAM', 'ROLE_OPERATIONAL_AUDIT_SENIOR_MANAGER', 'ROLE_OPERATIONAL_AUDIT_DIRECTOR']
  };
}
