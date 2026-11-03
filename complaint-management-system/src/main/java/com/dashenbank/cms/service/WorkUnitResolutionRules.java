package com.dashenbank.cms.service;

import java.util.Map;

/** Mandatory-field rules for the Work Unit resolution step (FormTask_57). */
public final class WorkUnitResolutionRules {

    public static final String KEY_RESOLUTION_DETAILS = "resolutionDetails";
    public static final String KEY_ACTION_TAKEN = "actionTaken";
    private static final String SECONDARY_REVIEW_NAME = "Secondary Resolution Review";

    private WorkUnitResolutionRules() {
    }

    public static boolean isSecondaryReview(String taskName) {
        return taskName != null && taskName.contains(SECONDARY_REVIEW_NAME);
    }

    /**
     * @return a user-facing message when a mandatory field is empty, or {@code null} when acceptable.
     *         Other tasks are never validated here.
     */
    public static String validate(String taskDefinitionKey, String taskName, Map<String, Object> variables) {
        if (!SlaAlertScope.TASK_WORK_UNIT.equals(taskDefinitionKey)) {
            return null;
        }
        if (isBlank(variables == null ? null : variables.get(KEY_RESOLUTION_DETAILS))) {
            return isSecondaryReview(taskName)
                    ? "Secondary Investigation / Corrective Action Findings is required."
                    : "Resolution Details is required.";
        }
        if (!isSecondaryReview(taskName) && isBlank(variables.get(KEY_ACTION_TAKEN))) {
            return "Action Taken is required.";
        }
        return null;
    }

    private static boolean isBlank(Object value) {
        return value == null || value.toString().isBlank();
    }
}
