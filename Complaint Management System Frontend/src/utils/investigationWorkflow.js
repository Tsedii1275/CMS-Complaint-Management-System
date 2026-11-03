// Audit Investigation workflow (Requires Investigation):
//   CCO -> Customer Care Senior Manager -> Service Quality
//   Service Quality:   Send to Chief Experience Officer  | Return to Senior Manager
//   Chief Experience Officer: Send to Audit Investigation | Return to Service Quality
//   Service Quality (case returned by the CXO): Return to Senior Manager (no remarks needed)
//   Senior Manager (case returned): Close the case | Send to Service Quality (loop repeats)

export const TASK_SM_REVIEW = 'FormTask_SM_Review';
export const TASK_SQ_REVIEW = 'FormTask_SQ_Review';
export const TASK_SQ_RETURN = 'FormTask_SQ_Return';
export const TASK_SM_CLOSURE = 'FormTask_SM_Closure';
export const TASK_CXO = 'FormTask_CEX';

export const CXO_SEND_TO_AUDIT = 'SEND_TO_AUDIT';
export const CXO_RETURN_TO_SERVICE_QUALITY = 'RETURN_TO_SERVICE_QUALITY';
// Kept for cases that started before the review loop; the server maps it for those cases.
export const CXO_RETURN_TO_SENIOR_MANAGER = 'RETURN_TO_SENIOR_MANAGER';

export const SQ_SEND_TO_CXO = 'SEND_TO_CXO';
export const SQ_RETURN_TO_SENIOR_MANAGER = 'RETURN_TO_SENIOR_MANAGER';

export const SM_CLOSE = 'CLOSE';
export const SM_SEND_TO_SERVICE_QUALITY = 'SEND_TO_SERVICE_QUALITY';

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
    actionLabel: 'Send to Chief Experience Officer',
    successText: 'Case reviewed and sent to the Chief Experience Officer.',
    remarksLabel: 'Service Quality Remarks',
    remarksPlaceholder: 'Enter your review remarks for the Chief Experience Officer...',
    remarksRequired: false,
    nextOwner: 'Chief Experience Officer or Customer Care Senior Manager',
    returnLabel: 'Return to Senior Manager',
    returnReasonLabel: 'Return reason',
    returnReasonHint: '(required only when returning)',
    returnReasonPlaceholder: 'Why this case is being returned to the Customer Care Senior Manager',
    returnSuccessText: 'Case returned to the Customer Care Senior Manager.'
  },
  [TASK_SQ_RETURN]: {
    label: 'Return to Senior Manager',
    modalTitle: 'Returned by the Chief Experience Officer - Service Quality',
    actionLabel: 'Return to Senior Manager',
    successText: 'Case returned to the Customer Care Senior Manager.',
    noInput: true,
    nextOwner: 'Customer Care Senior Manager'
  },
  [TASK_SM_CLOSURE]: {
    label: 'Return Review',
    modalTitle: 'Returned Case - Customer Care Senior Manager',
    actionLabel: 'Close Case & Notify Customer',
    successText: 'Case closed without audit investigation and the customer has been notified.',
    remarksLabel: 'Closure Summary (shared with the customer)',
    remarksPlaceholder: 'Summarise the outcome that will be sent to the customer...',
    remarksRequired: false,
    closureSummaryRequired: true,
    nextOwner: 'Customer or Service Quality Department',
    returnLabel: 'Send to Service Quality',
    returnReasonLabel: 'Investigation notes',
    returnReasonHint: '(required only when sending to Service Quality)',
    returnReasonPlaceholder: 'What you found after investigating, for the Service Quality Department',
    returnSuccessText: 'Case sent back to the Service Quality Department.'
  }
};

export const TRAIL_ACTION_LABELS = {
  REQUIRES_INVESTIGATION_SELECTED: 'Marked as Requires Investigation by Customer Care Officer',
  FORWARDED_TO_SERVICE_QUALITY: 'Senior Manager forwarded to Service Quality',
  FORWARDED_TO_CHIEF_EXPERIENCE_OFFICER: 'Service Quality forwarded to Chief Experience Officer',
  SENT_TO_AUDIT_INVESTIGATION: 'Chief Experience Officer sent to Audit Investigation',
  RETURNED_TO_SERVICE_QUALITY: 'Chief Experience Officer returned to Service Quality',
  RETURNED_TO_SENIOR_MANAGER: 'Returned to Customer Care Senior Manager',
  RESENT_TO_SERVICE_QUALITY: 'Senior Manager sent back to Service Quality',
  CLOSED_WITHOUT_AUDIT: 'Senior Manager closed the case without audit'
};

// Tag shown on a case row only while the case sits with someone because it was sent back.
export const RETURN_TAGS = {
  RETURNED_BY_SERVICE_QUALITY: { label: 'Returned by Service Quality', color: 'volcano' },
  RETURNED_BY_CXO: { label: 'Returned by CXO', color: 'magenta' },
  RETURNED_TO_SENIOR_MANAGER: { label: 'Returned to Senior Manager', color: 'orange' },
  RESENT_BY_SENIOR_MANAGER: { label: 'Resent by Senior Manager', color: 'purple' }
};

const CHAIN_TASK_KEYS = [TASK_SM_REVIEW, TASK_SQ_REVIEW, TASK_SQ_RETURN, TASK_SM_CLOSURE, TASK_CXO];

/** Returns { key, label, color, reason, by } for a sent-back case, or null for everything else. */
export function returnTagOf(task) {
  if (!task || !CHAIN_TASK_KEYS.includes(task.definitionKey)) {
    return null;
  }
  const vars = task.variables || {};
  const tag = RETURN_TAGS[String(vars.investigationReturnTag || '').trim()];
  if (!tag) {
    return null;
  }
  return {
    key: vars.investigationReturnTag,
    label: tag.label,
    color: tag.color,
    reason: String(vars.investigationReturnReason || '').trim(),
    by: String(vars.investigationReturnedBy || '').trim()
  };
}

export function isInvestigationReviewTask(task) {
  return Boolean(task && INVESTIGATION_STEPS[task.definitionKey]);
}

export function investigationTaskKeysForRole(role) {
  switch (role) {
    case 'ROLE_CUSTOMER_CARE_SENIOR_MANAGER':
      return [TASK_SM_REVIEW, TASK_SM_CLOSURE];
    case 'ROLE_SERVICE_QUALITY_DIRECTOR':
      return [TASK_SQ_REVIEW, TASK_SQ_RETURN];
    case 'ROLE_ADMIN':
      return [TASK_SM_REVIEW, TASK_SQ_REVIEW, TASK_SQ_RETURN, TASK_SM_CLOSURE];
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

/** Payload for the Senior Manager first review. */
export function buildReviewPayload(remarks, files) {
  return {
    reviewRemarks: (remarks || '').trim(),
    reviewFiles: files || []
  };
}

/** Payload for the Service Quality review: send to the CXO or return to the Senior Manager. */
export function buildSqDecisionPayload(decision, { remarks, reason, files }) {
  if (decision === SQ_RETURN_TO_SENIOR_MANAGER) {
    return {
      sqDecision: SQ_RETURN_TO_SENIOR_MANAGER,
      sqReturnReason: (reason || '').trim(),
      reviewRemarks: (remarks || '').trim(),
      reviewFiles: files || []
    };
  }
  return {
    sqDecision: SQ_SEND_TO_CXO,
    reviewRemarks: (remarks || '').trim(),
    reviewFiles: files || []
  };
}

/** Payload for the Senior Manager on a returned case: close it or send it back to Service Quality. */
export function buildSmDecisionPayload(decision, { summary, notes, files }) {
  if (decision === SM_SEND_TO_SERVICE_QUALITY) {
    return {
      smDecision: SM_SEND_TO_SERVICE_QUALITY,
      smResendReason: (notes || '').trim(),
      reviewFiles: files || []
    };
  }
  return {
    smDecision: SM_CLOSE,
    reviewRemarks: (summary || '').trim(),
    reviewFiles: files || []
  };
}

/** Payload for the Chief Experience Officer decision. */
export function buildCxoDecisionPayload(decision, { remarks, reason, files, existingFiles = [] }) {
  if (decision === CXO_RETURN_TO_SERVICE_QUALITY || decision === CXO_RETURN_TO_SENIOR_MANAGER) {
    return {
      requiresInvestigation: true,
      cxoDecision: decision,
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
