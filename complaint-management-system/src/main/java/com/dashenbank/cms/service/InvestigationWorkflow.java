package com.dashenbank.cms.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Pure helpers for the Audit Investigation workflow.
 *
 * <pre>
 * CCO "Requires Investigation"
 *   -> Customer Care Senior Manager review   (FormTask_SM_Review)
 *   -> Service Quality Department review     (FormTask_SQ_Review)
 *   -> Chief Experience Officer review       (FormTask_CEX)
 *        |- SEND_TO_AUDIT              -> Audit Investigation Team (FormTask_48)
 *        '- RETURN_TO_SENIOR_MANAGER   -> Senior Manager closure   (FormTask_SM_Closure)
 * </pre>
 */
public final class InvestigationWorkflow {

    public static final String KEY_CXO_DECISION = "cxoDecision";
    public static final String KEY_CXO_RETURN_REASON = "cxoReturnReason";
    public static final String KEY_REVIEW_REMARKS = "reviewRemarks";
    public static final String KEY_REVIEW_FILES = "reviewFiles";
    public static final String KEY_TRAIL = "investigationReviewTrail";
    public static final String KEY_INVESTIGATION_FILES = "investigationFiles";

    public static final String CXO_SEND_TO_AUDIT = "SEND_TO_AUDIT";
    public static final String CXO_RETURN_TO_SENIOR_MANAGER = "RETURN_TO_SENIOR_MANAGER";

    /** BPMN element that only exists in process versions that contain the review chain. */
    public static final String CLOSURE_TASK_ELEMENT = SlaAlertScope.TASK_SM_CLOSURE;

    private static final int MAX_TEXT_LENGTH = 4000;
    private static final ZoneId SYSTEM_ZONE = ZoneId.of("Africa/Addis_Ababa");

    private InvestigationWorkflow() {
    }

    /** Defaults to {@link #CXO_SEND_TO_AUDIT} so legacy payloads keep their previous behaviour. */
    public static String normalizeCxoDecision(Object raw) {
        if (raw == null) {
            return CXO_SEND_TO_AUDIT;
        }
        String value = raw.toString().trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        if (CXO_RETURN_TO_SENIOR_MANAGER.equals(value) || "RETURN".equals(value)) {
            return CXO_RETURN_TO_SENIOR_MANAGER;
        }
        return CXO_SEND_TO_AUDIT;
    }

    public static boolean isReviewChainTask(String taskDefinitionKey) {
        return SlaAlertScope.TASK_SM_REVIEW.equals(taskDefinitionKey)
                || SlaAlertScope.TASK_SQ_REVIEW.equals(taskDefinitionKey)
                || SlaAlertScope.TASK_SM_CLOSURE.equals(taskDefinitionKey);
    }

    /** True when the completion is handled by the dedicated review-chain logic. */
    public static boolean isHandledByChain(String taskDefinitionKey, Map<String, Object> variables) {
        if (isReviewChainTask(taskDefinitionKey)) {
            return true;
        }
        return SlaAlertScope.TASK_CXO.equals(taskDefinitionKey)
                && CXO_RETURN_TO_SENIOR_MANAGER.equals(
                        normalizeCxoDecision(variables != null ? variables.get(KEY_CXO_DECISION) : null));
    }

    /**
     * @return a user-facing validation message, or {@code null} when the payload is acceptable.
     */
    public static String validate(String taskDefinitionKey, Map<String, Object> variables) {
        if (variables == null) {
            return null;
        }
        if (SlaAlertScope.TASK_SM_CLOSURE.equals(taskDefinitionKey)
                && text(variables.get(KEY_REVIEW_REMARKS)).isBlank()) {
            return "A closure summary is required before closing the case and notifying the customer.";
        }
        if (!SlaAlertScope.TASK_CXO.equals(taskDefinitionKey)) {
            return null;
        }
        String decision = normalizeCxoDecision(variables.get(KEY_CXO_DECISION));
        if (CXO_RETURN_TO_SENIOR_MANAGER.equals(decision)
                && text(variables.get(KEY_CXO_RETURN_REASON)).isBlank()) {
            return "A reason is required when returning the case to the Customer Care Senior Manager.";
        }
        return null;
    }

    public static String text(Object value) {
        if (value == null) {
            return "";
        }
        String s = value.toString().trim();
        return s.length() > MAX_TEXT_LENGTH ? s.substring(0, MAX_TEXT_LENGTH) : s;
    }

    public static Map<String, Object> trailEntry(String step, String action, String actor, String role,
            String remarks, int fileCount) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("step", step);
        entry.put("action", action);
        entry.put("actor", actor);
        entry.put("role", role);
        entry.put("remarks", text(remarks));
        entry.put("fileCount", fileCount);
        entry.put("at", LocalDateTime.now(SYSTEM_ZONE).toString());
        return entry;
    }

    /** Returns a new serializable list holding the existing trail plus the new entry. */
    public static ArrayList<Map<String, Object>> appendTrail(Object existing, Map<String, Object> entry) {
        ArrayList<Map<String, Object>> trail = new ArrayList<>();
        if (existing instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    Map<String, Object> copy = new HashMap<>();
                    map.forEach((k, v) -> copy.put(String.valueOf(k), v));
                    trail.add(copy);
                }
            }
        }
        trail.add(entry);
        return trail;
    }

    /** Existing investigation files plus newly uploaded review files (never null). */
    public static ArrayList<Object> mergeFiles(Object existing, Object added) {
        ArrayList<Object> merged = new ArrayList<>();
        if (existing instanceof List<?> list) {
            merged.addAll(list);
        }
        if (added instanceof List<?> list) {
            merged.addAll(list);
        }
        return merged;
    }

    public static int countFiles(Object files) {
        return files instanceof List<?> list ? list.size() : 0;
    }
}
