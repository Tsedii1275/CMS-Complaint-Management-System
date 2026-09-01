package com.dashenbank.cms.model;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Official overall complaint status. Workflow/SLA stage names stay separate.
 */
public final class OverallComplaintStatus {

    public static final String RECORDED = "RECORDED";
    public static final String ESCALATED = "ESCALATED";
    public static final String ON_TRACK = "ON_TRACK";
    public static final String RESOLVED = "RESOLVED";
    public static final String CLOSED = "CLOSED";
    public static final String DECLINED = "DECLINED";

    private static final Set<String> OFFICIAL = Set.of(RECORDED, ESCALATED, ON_TRACK, RESOLVED, CLOSED, DECLINED);

    private OverallComplaintStatus() {
    }

    public static boolean isOfficial(String status) {
        return status != null && OFFICIAL.contains(status.trim().toUpperCase(Locale.ROOT));
    }

    public static String canonicalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return RECORDED;
        }
        String upper = raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        if ("ONTRACK".equals(upper) || ON_TRACK.equals(upper)) {
            return ON_TRACK;
        }
        if (isOfficial(upper)) {
            return upper;
        }
        return resolveFromTokens(upper, false);
    }

    public static String displayLabel(String status) {
        return switch (canonicalize(status)) {
            case ESCALATED -> "Escalated";
            case ON_TRACK -> "On_Track";
            case RESOLVED -> "Resolved";
            case CLOSED -> "Closed";
            case DECLINED -> "Declined";
            default -> "Recorded";
        };
    }

    /**
     * Maps live workflow variables onto the six official overall statuses.
     */
    public static String resolve(Map<String, Object> vars, boolean customerFeedbackSubmitted) {
        if (vars == null || vars.isEmpty()) {
            return customerFeedbackSubmitted ? CLOSED : RECORDED;
        }

        String classification = str(vars, "classification");
        String decision = str(vars, "decision");
        String status = str(vars, "status");
        String currentStage = str(vars, "currentStage");
        String stage = str(vars, "stage");
        String committeeStatus = str(vars, "committeeStatus");

        if (containsDeclined(classification) || containsDeclined(decision) || containsDeclined(status)) {
            return DECLINED;
        }

        if (customerFeedbackSubmitted) {
            return CLOSED;
        }

        if (isCustomerCareOfficerClosure(vars, status, currentStage, stage)
                || isFcrCloseCase(vars, status, currentStage, stage)) {
            return RESOLVED;
        }

        if (isInvestigationPath(vars, currentStage, stage, committeeStatus, status)) {
            return ESCALATED;
        }

        if (isWorkUnitAssigned(vars, currentStage, stage, status)) {
            return ON_TRACK;
        }

        if (isRecordedStage(currentStage, stage, status)) {
            return RECORDED;
        }

        return canonicalize((status == null || status.isBlank()) ? RECORDED : status);
    }

    private static boolean isCustomerCareOfficerClosure(Map<String, Object> vars, String status, String currentStage,
            String stage) {
        if (Boolean.TRUE.equals(vars.get("resolutionAccepted"))) {
            return true;
        }
        return CLOSED.equalsIgnoreCase(status)
                || CLOSED.equalsIgnoreCase(currentStage)
                || CLOSED.equalsIgnoreCase(stage);
    }

    /**
     * Workflow payload from "Approve FCR & Close Case". Boolean {@code fcrStatus}
     * is not used here because committee close also sends {@code fcrStatus: true}.
     */
    public static boolean isFcrVerifiedClose(Map<String, Object> vars) {
        if (vars == null || vars.isEmpty()) {
            return false;
        }
        String fcrStatus = str(vars, "fcrStatus");
        String decision = str(vars, "decision");
        String fcrAction = str(vars, "fcrAction");
        String status = str(vars, "status");
        String currentStage = str(vars, "currentStage");
        String stage = str(vars, "stage");
        if ("VERIFIED".equalsIgnoreCase(fcrStatus) || "FCR_APPROVED".equalsIgnoreCase(decision)) {
            return true;
        }
        return "approve".equalsIgnoreCase(fcrAction)
                && (RESOLVED.equalsIgnoreCase(status)
                        || RESOLVED.equalsIgnoreCase(currentStage)
                        || RESOLVED.equalsIgnoreCase(stage));
    }

    private static boolean isFcrCloseCase(Map<String, Object> vars, String status, String currentStage,
            String stage) {
        Object rawFcr = vars.get("fcrStatus");
        if (Boolean.TRUE.equals(rawFcr)
                || (rawFcr instanceof Number && ((Number) rawFcr).intValue() == 1)) {
            return true;
        }
        String fcrStatus = str(vars, "fcrStatus");
        if ("TRUE".equalsIgnoreCase(fcrStatus)) {
            return true;
        }
        return isFcrVerifiedClose(vars);
    }

    private static boolean isInvestigationPath(Map<String, Object> vars, String currentStage, String stage,
            String committeeStatus, String status) {
        if (Boolean.TRUE.equals(vars.get("requiresInvestigation"))) {
            return true;
        }
        String blob = (currentStage + " " + stage + " " + committeeStatus + " " + status).toUpperCase(Locale.ROOT);
        return blob.contains("INVESTIGAT")
                || blob.contains("AUDIT")
                || blob.contains("COMMITTEE")
                || blob.contains("CHIEF_EXPERIENCE")
                || blob.contains("CHIEF_OPERATION")
                || blob.contains("ESCALAT");
    }

    private static boolean isWorkUnitAssigned(Map<String, Object> vars, String currentStage, String stage,
            String status) {
        String blob = (currentStage + " " + stage + " " + status).toUpperCase(Locale.ROOT);
        if (blob.contains("WORK_UNIT") || blob.contains(ON_TRACK) || blob.contains("ONTRACK")
                || blob.contains("RESOLUTION_GIVEN") || blob.contains("ASSIGN")) {
            return true;
        }
        return hasText(vars.get("assignedDepartment")) || hasText(vars.get("assignedBranch"))
                || hasText(vars.get("department"));
    }

    private static boolean isRecordedStage(String currentStage, String stage, String status) {
        String blob = (currentStage + " " + stage + " " + status).toUpperCase(Locale.ROOT);
        return blob.contains("CMD_SCREENING") || blob.contains(RECORDED) || blob.contains("INTAKE")
                || blob.contains("NEW") || blob.isBlank();
    }

    private static boolean containsDeclined(String value) {
        return value != null && value.toUpperCase(Locale.ROOT).contains("DECLINE");
    }

    private static boolean hasText(Object value) {
        return value != null && !value.toString().isBlank();
    }

    private static String str(Map<String, Object> vars, String key) {
        Object v = vars.get(key);
        return v == null ? "" : v.toString().trim();
    }

    private static String resolveFromTokens(String upper, boolean feedbackSubmitted) {
        if (feedbackSubmitted || "CASE_CLOSED_BY_CUSTOMER".equals(upper)) {
            return CLOSED;
        }
        if (upper.contains("DECLINE") || "REJECTED".equals(upper)) {
            return DECLINED;
        }
        if (upper.contains("CLOSE") || upper.contains("COMPLETED") || upper.contains("FINAL")) {
            return RESOLVED;
        }
        if (upper.contains("RESOLV") || upper.contains("SOLV")) {
            return RESOLVED;
        }
        if (upper.contains("ESCALAT") || upper.contains("INVESTIGAT") || upper.contains("COMMITTEE")
                || upper.contains("AUDIT")) {
            return ESCALATED;
        }
        if (upper.contains("TRACK") || upper.contains("PROGRESS") || upper.contains("ASSIGN")
                || upper.contains("WORK_UNIT") || upper.contains("PENDING") || upper.contains("OPEN")) {
            return ON_TRACK;
        }
        return RECORDED;
    }
}
