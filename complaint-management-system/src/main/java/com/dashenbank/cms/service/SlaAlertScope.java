package com.dashenbank.cms.service;

import com.dashenbank.cms.model.Role;
import com.dashenbank.cms.model.User;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Reusable mapping of workflow task keys to SLA stages and of roles to the
 * stages/tasks they are authorized to see. Used for SLA alerts, task lists,
 * and SLA metric APIs so visibility is never inferred from ticket number or
 * overall complaint status alone.
 */
public final class SlaAlertScope {

    public static final String STAGE_BRANCH_INTAKE = "BRANCH_INTAKE";
    public static final String STAGE_CONTACT_CENTER_INTAKE = "CONTACT_CENTER_INTAKE";
    public static final String STAGE_CUSTOMER_NOTIFICATION = "CUSTOMER_NOTIFICATION";
    public static final String STAGE_CMD_SCREENING = "CMD_SCREENING";
    public static final String STAGE_INVESTIGATION = "INVESTIGATION";
    public static final String STAGE_WORK_UNIT_RESOLUTION = "WORK_UNIT_RESOLUTION";
    public static final String STAGE_SERVICE_QUALITY_REVIEW = "SERVICE_QUALITY_REVIEW";
    public static final String STAGE_COMMITTEE_REVIEW = "COMMITTEE_REVIEW";
    public static final String STAGE_CHIEF_EXPERIENCE_REVIEW = "CHIEF_EXPERIENCE_REVIEW";

    public static final String TASK_BRANCH_CAPTURE = "FormTask_12";
    public static final String TASK_BRANCH_REVIEW = "FormTask_16";
    public static final String TASK_CONTACT_CENTER = "FormTask_20";
    public static final String TASK_BRANCH_FOLLOWUP = "FormTask_24";
    public static final String TASK_CUSTOMER_NOTIFY = "FormTask_67";
    public static final String TASK_CMD_SCREENING = "FormTask_43";
    public static final String TASK_INVESTIGATION = "FormTask_48";
    public static final String TASK_WORK_UNIT = "FormTask_57";
    public static final String TASK_SERVICE_QUALITY = "ServiceTask_65";
    public static final String TASK_COMMITTEE = "FormTask_ChiefCommittee";
    public static final String TASK_CXO = "FormTask_CEX";

    private static final Map<String, String> TASK_TO_STAGE = new HashMap<>();
    private static final Map<String, String> STAGE_LABELS = new HashMap<>();
    private static final Map<String, String> STAGE_SLA_NAMES = new HashMap<>();

    static {
        TASK_TO_STAGE.put(TASK_BRANCH_CAPTURE, STAGE_BRANCH_INTAKE);
        TASK_TO_STAGE.put(TASK_BRANCH_REVIEW, STAGE_BRANCH_INTAKE);
        TASK_TO_STAGE.put(TASK_BRANCH_FOLLOWUP, STAGE_BRANCH_INTAKE);
        TASK_TO_STAGE.put(TASK_CONTACT_CENTER, STAGE_CONTACT_CENTER_INTAKE);
        TASK_TO_STAGE.put(TASK_CUSTOMER_NOTIFY, STAGE_CUSTOMER_NOTIFICATION);
        TASK_TO_STAGE.put(TASK_CMD_SCREENING, STAGE_CMD_SCREENING);
        TASK_TO_STAGE.put(TASK_INVESTIGATION, STAGE_INVESTIGATION);
        TASK_TO_STAGE.put(TASK_WORK_UNIT, STAGE_WORK_UNIT_RESOLUTION);
        TASK_TO_STAGE.put(TASK_SERVICE_QUALITY, STAGE_SERVICE_QUALITY_REVIEW);
        TASK_TO_STAGE.put(TASK_COMMITTEE, STAGE_COMMITTEE_REVIEW);
        TASK_TO_STAGE.put(TASK_CXO, STAGE_CHIEF_EXPERIENCE_REVIEW);

        STAGE_LABELS.put(STAGE_BRANCH_INTAKE, "Branch Intake / Resolution");
        STAGE_LABELS.put(STAGE_CONTACT_CENTER_INTAKE, "Contact Center Intake");
        STAGE_LABELS.put(STAGE_CUSTOMER_NOTIFICATION, "Customer Care Notification");
        STAGE_LABELS.put(STAGE_CMD_SCREENING, "Customer Care Screening");
        STAGE_LABELS.put(STAGE_INVESTIGATION, "Audit / Investigation");
        STAGE_LABELS.put(STAGE_WORK_UNIT_RESOLUTION, "Department / Work Unit Resolution");
        STAGE_LABELS.put(STAGE_SERVICE_QUALITY_REVIEW, "Service Quality Review");
        STAGE_LABELS.put(STAGE_COMMITTEE_REVIEW, "Chief Committee Review");
        STAGE_LABELS.put(STAGE_CHIEF_EXPERIENCE_REVIEW, "Chief Experience Officer Review");

        STAGE_SLA_NAMES.put(STAGE_BRANCH_INTAKE, "Branch SLA");
        STAGE_SLA_NAMES.put(STAGE_CONTACT_CENTER_INTAKE, "Contact Center SLA");
        STAGE_SLA_NAMES.put(STAGE_CUSTOMER_NOTIFICATION, "Customer Care Notification SLA");
        STAGE_SLA_NAMES.put(STAGE_CMD_SCREENING, "Customer Care Screening SLA");
        STAGE_SLA_NAMES.put(STAGE_INVESTIGATION, "Investigation SLA");
        STAGE_SLA_NAMES.put(STAGE_WORK_UNIT_RESOLUTION, "Work Unit Resolution SLA");
        STAGE_SLA_NAMES.put(STAGE_SERVICE_QUALITY_REVIEW, "Service Quality SLA");
        STAGE_SLA_NAMES.put(STAGE_COMMITTEE_REVIEW, "Committee Review SLA");
        STAGE_SLA_NAMES.put(STAGE_CHIEF_EXPERIENCE_REVIEW, "Executive Review SLA");
    }

    private SlaAlertScope() {
    }

    public static String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "";
        }
        String trimmed = role.trim();
        if (!trimmed.startsWith("ROLE_")) {
            return "ROLE_" + trimmed.toUpperCase(Locale.ROOT);
        }
        return trimmed;
    }

    public static boolean isAdmin(String role) {
        return Role.ROLE_ADMIN.name().equals(normalizeRole(role));
    }

    public static String stageCodeFromTaskKey(String taskDefinitionKey) {
        if (taskDefinitionKey == null || taskDefinitionKey.isBlank()) {
            return null;
        }
        return TASK_TO_STAGE.get(taskDefinitionKey);
    }

    public static String stageLabel(String stageCode) {
        if (stageCode == null || stageCode.isBlank()) {
            return "Workflow Processing";
        }
        String canonical = canonicalizeStage(stageCode);
        return STAGE_LABELS.getOrDefault(canonical, humanize(stageCode));
    }

    public static String slaNameForStage(String stageCode) {
        String canonical = canonicalizeStage(stageCode);
        if (canonical == null) {
            return "Stage SLA";
        }
        return STAGE_SLA_NAMES.getOrDefault(canonical, "Stage SLA");
    }

    public static String canonicalizeStage(String stageCode) {
        if (stageCode == null || stageCode.isBlank()) {
            return null;
        }
        String upper = stageCode.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        if (isWorkUnitStage(upper)) {
            return STAGE_WORK_UNIT_RESOLUTION;
        }
        if (isInvestigationStage(upper)) {
            return STAGE_INVESTIGATION;
        }
        if (upper.contains("SERVICE_QUALITY")) {
            return STAGE_SERVICE_QUALITY_REVIEW;
        }
        if (upper.contains("COMMITTEE")) {
            return STAGE_COMMITTEE_REVIEW;
        }
        if (isChiefExperienceStage(upper)) {
            return STAGE_CHIEF_EXPERIENCE_REVIEW;
        }
        if (upper.contains("CONTACT_CENTER")) {
            return STAGE_CONTACT_CENTER_INTAKE;
        }
        if (upper.contains("BRANCH")) {
            return STAGE_BRANCH_INTAKE;
        }
        if (isCustomerNotificationStage(upper)) {
            return STAGE_CUSTOMER_NOTIFICATION;
        }
        if (upper.contains("CMD") || upper.contains("SCREENING")) {
            return STAGE_CMD_SCREENING;
        }
        return upper;
    }

    private static boolean isWorkUnitStage(String upper) {
        return upper.contains("WORK_UNIT") || upper.contains("SECOND_LEVEL")
                || STAGE_WORK_UNIT_RESOLUTION.equals(upper) || "DEPARTMENT_WORKUNIT".equals(upper);
    }

    private static boolean isInvestigationStage(String upper) {
        return upper.contains(STAGE_INVESTIGATION) || "AUDIT_TEAM".equals(upper);
    }

    private static boolean isChiefExperienceStage(String upper) {
        return upper.contains("CHIEF_EXPERIENCE") || upper.contains("CXO") || upper.contains("CEX")
                || "CXO_REVIEW".equals(upper) || "CEO_DIRECTION".equals(upper);
    }

    private static boolean isCustomerNotificationStage(String upper) {
        return upper.contains(STAGE_CUSTOMER_NOTIFICATION) || "NOTIFICATION".equals(upper);
    }

    public static boolean sameStage(String left, String right) {
        String a = canonicalizeStage(left);
        String b = canonicalizeStage(right);
        return a != null && a.equals(b);
    }

    public static Set<String> authorizedTaskKeys(String role) {
        String normalized = normalizeRole(role);
        if (isAdmin(normalized)) {
            return Set.copyOf(TASK_TO_STAGE.keySet());
        }
        if (normalized.contains("AUDIT")) {
            return Set.of(TASK_INVESTIGATION);
        }
        return switch (normalized) {
            case "ROLE_CUSTOMER_CARE_OFFICER", "ROLE_CUSTOMER_CARE_TEAM_LEADER",
                    "ROLE_CUSTOMER_CARE_SENIOR_MANAGER" ->
                Set.of(TASK_CMD_SCREENING, TASK_CUSTOMER_NOTIFY);
            case "ROLE_SERVICE_QUALITY_DIRECTOR" -> Set.of(TASK_SERVICE_QUALITY);
            case "ROLE_DEPARTMENT_WORKUNIT" -> Set.of(TASK_WORK_UNIT);
            case "ROLE_CHIEF_COMMITTEE", "ROLE_COMMITTEE_SECRETARY" -> Set.of(TASK_COMMITTEE);
            case "ROLE_CHIEF_EXPERIENCE_OFFICER" -> Set.of(TASK_CXO);
            case "ROLE_BRANCH_MANAGER", "ROLE_CUSTOMER_SERVICE_MANAGER",
                    "ROLE_CUSTOMER_EXPERIENCE_PARTNERSHIP" ->
                Set.of(TASK_BRANCH_CAPTURE, TASK_BRANCH_REVIEW, TASK_BRANCH_FOLLOWUP);
            case "ROLE_CONTACT_CENTER_AGENT", "ROLE_CONTACT_CENTER_SENIOR_MANAGER",
                    "ROLE_DIGITAL_MARKETING_OFFICER", "ROLE_DIGITAL_MARKETING_SENIOR_MANAGER" ->
                Set.of(TASK_BRANCH_CAPTURE, TASK_BRANCH_REVIEW, TASK_CONTACT_CENTER, TASK_BRANCH_FOLLOWUP,
                        TASK_CUSTOMER_NOTIFY);
            default -> Set.of();
        };
    }

    public static Set<String> authorizedStageCodes(String role) {
        if (isAdmin(role)) {
            return Set.copyOf(STAGE_LABELS.keySet());
        }
        Set<String> stages = new java.util.HashSet<>();
        for (String key : authorizedTaskKeys(role)) {
            String stage = stageCodeFromTaskKey(key);
            if (stage != null) {
                stages.add(stage);
            }
        }
        return Collections.unmodifiableSet(stages);
    }

    public static boolean roleMayHandleTask(String role, String taskDefinitionKey) {
        if (isAdmin(role)) {
            return true;
        }
        Set<String> keys = authorizedTaskKeys(role);
        if (keys.isEmpty()) {
            return false;
        }
        if (taskDefinitionKey != null && keys.contains(taskDefinitionKey)) {
            return true;
        }
        return Role.ROLE_DEPARTMENT_WORKUNIT.name().equals(normalizeRole(role))
                && (taskDefinitionKey == null || "SecondaryResolutionReview".equals(taskDefinitionKey));
    }

    public static boolean canAssignTasks(String role) {
        String normalized = normalizeRole(role);
        return isAdmin(normalized)
                || "ROLE_CUSTOMER_CARE_OFFICER".equals(normalized)
                || "ROLE_CUSTOMER_CARE_TEAM_LEADER".equals(normalized)
                || "ROLE_CUSTOMER_CARE_SENIOR_MANAGER".equals(normalized)
                || "ROLE_SERVICE_QUALITY_DIRECTOR".equals(normalized);
    }

    public static boolean roleMaySeeStage(String role, String stageCode) {
        if (isAdmin(role)) {
            return true;
        }
        String canonical = canonicalizeStage(stageCode);
        if (canonical == null) {
            return false;
        }
        return authorizedStageCodes(role).contains(canonical);
    }

    /**
     * Organizational scope: a populated user attribute must match a populated
     * ticket attribute. Blank values on either side leave that dimension open.
     * Task/stage authorization is still required separately so a blank
     * department cannot expose another lane's SLA.
     */
    public static boolean matchesOrganizationalScope(User user, String branch, String district, String department) {
        if (user == null) {
            return false;
        }
        if (!dimensionMatches(user.getBranch(), branch)) {
            return false;
        }
        if (!dimensionMatches(user.getDistrict(), district)) {
            return false;
        }
        return dimensionMatches(user.getDepartment(), department);
    }

    private static boolean dimensionMatches(String userValue, String ticketValue) {
        if (userValue == null || userValue.isBlank() || ticketValue == null || ticketValue.isBlank()) {
            return true;
        }
        return userValue.trim().equalsIgnoreCase(ticketValue.trim());
    }

    private static String humanize(String stageCode) {
        String[] parts = stageCode.toLowerCase(Locale.ROOT).replace('_', ' ').split(" ");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }
}
