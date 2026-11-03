export const WORK_UNIT_MAX_TEXT_LENGTH = 4000;

export function isSecondaryReviewTask(task) {
  return Boolean(task?.name?.includes('Secondary Resolution Review'));
}

/** Returns { field: message } for every mandatory field that is still empty. */
export function validateResolutionForm(formData, isSecondaryReview) {
  const errors = {};
  if (!String(formData?.resolutionDetails || '').trim()) {
    errors.resolutionDetails = isSecondaryReview
      ? 'Secondary Investigation / Corrective Action Findings is required.'
      : 'Resolution Details is required.';
  }
  if (!isSecondaryReview && !String(formData?.actionTaken || '').trim()) {
    errors.actionTaken = 'Action Taken is required.';
  }
  return errors;
}
