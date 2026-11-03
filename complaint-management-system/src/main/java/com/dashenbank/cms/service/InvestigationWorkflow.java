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
 *        |- SEND_TO_CXO                -> Chief Experience Officer review (FormTask_CEX)
 *        '- RETURN_TO_SENIOR_MANAGER   -> Senior Manager return review    (FormTask_SM_Closure)
 *   Chief Experience Officer review       (FormTask_CEX)
 *        |- SEND_TO_AUDIT              -> Audit Investigation Team (FormTask_48)
 *        '- RETURN_TO_SERVICE_QUALITY  -> Service Quality return step     (FormTask_SQ_Return)
 *   Service Quality return step           (FormTask_SQ_Return)
 *        '- always                     -> Senior Manager return review    (FormTask_SM_Closure)
 *   Senior Manager return review          (FormTask_SM_Closure)
 *        |- CLOSE                      -> case closed, customer notified
 *        '- SEND_TO_SERVICE_QUALITY    -> Service Quality review (FormTask_SQ_Review), loop repeats
 * </pre>
 */
public final class InvestigationWorkflow {

    public static final String KEY_CXO_DECISION = "cxoDecision";
    public static final String KEY_CXO_RETURN_REASON = "cxoReturnReason";
    public static final String KEY_SQ_DECISION = "sqDecision";
    public static final String KEY_SQ_RETURN_REASON = "sqReturnReason";
    public static final String KEY_SM_DECISION = "smDecision";
    public static final String KEY_SM_RESEND_REASON = "smResendReason";
    public static final String KEY_REVIEW_REMARKS = "reviewRemarks";
    public static final String KEY_REVIEW_FILES = "reviewFiles";
    public static final String KEY_TRAIL = "investigationReviewTrail";
    public static final String KEY_INVESTIGATION_FILES = "investigationFiles";
    /** Set while a case sits with someone because it was sent back; cleared when it moves on. */
    public static final String KEY_RETURN_TAG = "investigationReturnTag";
    public static final String KEY_RETURN_REASON = "investigationReturnReason";
    public static final String KEY_RETURNED_BY = "investigationReturnedBy";

    public static final String CXO_SEND_TO_AUDIT = "SEND_TO_AUDIT";
    /** Original route; only used by process versions deployed before the review loop. */
    public static final String CXO_RETURN_TO_SENIOR_MANAGER = "RETURN_TO_SENIOR_MANAGER";
    public static final String CXO_RETURN_TO_SERVICE_QUALITY = "RETURN_TO_SERVICE_QUALITY";

    public static final String SQ_SEND_TO_CXO = "SEND_TO_CXO";
    public static final String SQ_RETURN_TO_SENIOR_MANAGER = "RETURN_TO_SENIOR_MANAGER";

    public static final String SM_CLOSE = "CLOSE";
    public static final String SM_SEND_TO_SERVICE_QUALITY = "SEND_TO_SERVICE_QUALITY";

    public static final String TAG_RETURNED_BY_SERVICE_QUALITY = "RETURNED_BY_SERVICE_QUALITY";
    public static final String TAG_RETURNED_BY_CXO = "RETURNED_BY_CXO";
    public static final String TAG_RETURNED_TO_SENIOR_MANAGER = "RETURNED_TO_SENIOR_MANAGER";
    public static final String TAG_RESENT_BY_SENIOR_MANAGER = "RESENT_BY_SENIOR_MANAGER";

    /** BPMN element that only exists in process versions that contain the review chain. */
    public static final String CLOSURE_TASK_ELEMENT = SlaAlertScope.TASK_SM_CLOSURE;
    /** BPMN element that only exists in process versions that contain the review loop. */
    public static final String LOOP_TASK_ELEMENT = SlaAlertScope.TASK_SQ_RETURN;

    private static final int MAX_TEXT_LENGTH = 4000;
    private static final ZoneId SYSTEM_ZONE = ZoneId.of("Africa/Addis_Ababa");

    private InvestigationWorkflow() {
    }

    private static String token(Object raw) {
        return raw == null ? "" : raw.toString().trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    /** Defaults to {@link #CXO_SEND_TO_AUDIT} so legacy payloads keep their previous behaviour. */
    public static String normalizeCxoDecision(Object raw) {
        String value = token(raw);
        if (CXO_RETURN_TO_SERVICE_QUALITY.equals(value)) {
            return CXO_RETURN_TO_SERVICE_QUALITY;
        }
        if (CXO_RETURN_TO_SENIOR_MANAGER.equals(value) || "RETURN".equals(value)) {
            return CXO_RETURN_TO_SENIOR_MANAGER;
        }
        return CXO_SEND_TO_AUDIT;
    }

    public static boolean isCxoReturn(String normalizedDecision) {
        return CXO_RETURN_TO_SENIOR_MANAGER.equals(normalizedDecision)
                || CXO_RETURN_TO_SERVICE_QUALITY.equals(normalizedDecision);
    }

    /** Defaults to {@link #SQ_SEND_TO_CXO} so the original forward-only payload keeps working. */
    public static String normalizeSqDecision(Object raw) {
        String value = token(raw);
        if (SQ_RETURN_TO_SENIOR_MANAGER.equals(value) || "RETURN".equals(value)) {
            return SQ_RETURN_TO_SENIOR_MANAGER;
        }
        return SQ_SEND_TO_CXO;
    }

    /** Defaults to {@link #SM_CLOSE} so the original closure payload keeps working. */
    public static String normalizeSmDecision(Object raw) {
        String value = token(raw);
        if (SM_SEND_TO_SERVICE_QUALITY.equals(value) || "RESEND".equals(value)) {
            return SM_SEND_TO_SERVICE_QUALITY;
        }
        return SM_CLOSE;
    }

    public static boolean isReviewChainTask(String taskDefinitionKey) {
        return SlaAlertScope.TASK_SM_REVIEW.equals(taskDefinitionKey)
                || SlaAlertScope.TASK_SQ_REVIEW.equals(taskDefinitionKey)
                || SlaAlertScope.TASK_SQ_RETURN.equals(taskDefinitionKey)
                || SlaAlertScope.TASK_SM_CLOSURE.equals(taskDefinitionKey);
    }

    /** True when the completion is handled by the dedicated review-chain logic. */
    public static boolean isHandledByChain(String taskDefinitionKey, Map<String, Object> variables) {
        if (isReviewChainTask(taskDefinitionKey)) {
            return true;
        }
        return SlaAlertScope.TASK_CXO.equals(taskDefinitionKey)
                && isCxoReturn(normalizeCxoDecision(variables != null ? variables.get(KEY_CXO_DECISION) : null));
    }

    /** True when the step needs the review loop (only available on newer process versions). */
    public static boolean needsReviewLoop(String taskDefinitionKey, Map<String, Object> variables) {
        if (variables == null) {
            return SlaAlertScope.TASK_SQ_RETURN.equals(taskDefinitionKey);
        }
        return switch (taskDefinitionKey == null ? "" : taskDefinitionKey) {
            case SlaAlertScope.TASK_SQ_RETURN -> true;
            case SlaAlertScope.TASK_SQ_REVIEW ->
                SQ_RETURN_TO_SENIOR_MANAGER.equals(normalizeSqDecision(variables.get(KEY_SQ_DECISION)));
            case SlaAlertScope.TASK_SM_CLOSURE ->
                SM_SEND_TO_SERVICE_QUALITY.equals(normalizeSmDecision(variables.get(KEY_SM_DECISION)));
            case SlaAlertScope.TASK_CXO ->
                CXO_RETURN_TO_SERVICE_QUALITY.equals(normalizeCxoDecision(variables.get(KEY_CXO_DECISION)));
            default -> false;
        };
    }

    /**
     * @return a user-facing validation message, or {@code null} when the payload is acceptable.
     */
    public static String validate(String taskDefinitionKey, Map<String, Object> variables) {
        if (variables == null) {
            return null;
        }
        if (SlaAlertScope.TASK_SQ_REVIEW.equals(taskDefinitionKey)
                && SQ_RETURN_TO_SENIOR_MANAGER.equals(normalizeSqDecision(variables.get(KEY_SQ_DECISION)))
                && text(variables.get(KEY_SQ_RETURN_REASON)).isBlank()) {
            return "A reason is required when returning the case to the Customer Care Senior Manager.";
        }
        if (SlaAlertScope.TASK_SM_CLOSURE.equals(taskDefinitionKey)) {
            if (SM_SEND_TO_SERVICE_QUALITY.equals(normalizeSmDecision(variables.get(KEY_SM_DECISION)))) {
                return text(variables.get(KEY_SM_RESEND_REASON)).isBlank()
                        ? "Investigation notes are required when sending the case back to Service Quality."
                        : null;
            }
            if (text(variables.get(KEY_REVIEW_REMARKS)).isBlank()) {
                return "A closure summary is required before closing the case and notifying the customer.";
            }
        }
        if (!SlaAlertScope.TASK_CXO.equals(taskDefinitionKey)) {
            return null;
        }
        String decision = normalizeCxoDecision(variables.get(KEY_CXO_DECISION));
        if (isCxoReturn(decision) && text(variables.get(KEY_CXO_RETURN_REASON)).isBlank()) {
            return CXO_RETURN_TO_SERVICE_QUALITY.equals(decision)
                    ? "A reason is required when returning the case to Service Quality."
                    : "A reason is required when returning the case to the Customer Care Senior Manager.";
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

    /**
     * Existing files plus newly submitted ones, without listing the same file twice (matched by url,
     * then name). Used so a later resolution never drops documents attached earlier on the case.
     */
    public static ArrayList<Object> mergeFilesUnique(Object existing, Object added) {
        ArrayList<Object> merged = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (Object source : new Object[] { existing, added }) {
            if (!(source instanceof List<?> list)) {
                continue;
            }
            for (Object item : list) {
                String key = fileKey(item);
                if (key == null || seen.add(key)) {
                    merged.add(item);
                }
            }
        }
        return merged;
    }

    private static String fileKey(Object item) {
        if (item instanceof Map<?, ?> map) {
            Object url = map.get("url");
            if (url != null && !String.valueOf(url).isBlank()) {
                return String.valueOf(url);
            }
            Object name = map.get("name");
            return name == null ? null : String.valueOf(name);
        }
        return item == null ? null : String.valueOf(item);
    }

    public static int countFiles(Object files) {
        return files instanceof List<?> list ? list.size() : 0;
    }
}
