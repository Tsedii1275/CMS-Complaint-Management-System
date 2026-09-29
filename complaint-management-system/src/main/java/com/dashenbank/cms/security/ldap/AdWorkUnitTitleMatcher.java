package com.dashenbank.cms.security.ldap;

import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

/**
 * Normalized job-title and AD-group matching for Work Unit assignment scopes.
 */
public final class AdWorkUnitTitleMatcher {

    private AdWorkUnitTitleMatcher() {
    }

    public static boolean matchesTitle(String title, AdAssignmentScope scope) {
        String normalized = normalize(title);
        if (normalized.isEmpty()) {
            return false;
        }
        return switch (scope) {
            case DISTRICT -> isDistrictTitle(normalized);
            case BRANCH -> isBranchTitle(normalized);
            case HEAD_OFFICE_DEPARTMENT -> isHeadOfficeTitle(normalized);
        };
    }

    public static boolean matchesAnyWorkUnitTitle(String title) {
        String normalized = normalize(title);
        if (normalized.isEmpty()) {
            return false;
        }
        return isDistrictTitle(normalized) || isBranchTitle(normalized) || isHeadOfficeTitle(normalized);
    }

    public static boolean matchesGroup(List<String> memberOf, AdAssignmentScope scope) {
        if (memberOf == null || memberOf.isEmpty()) {
            return false;
        }
        for (String dn : memberOf) {
            if (matchesTitle(groupKey(dn), scope)) {
                return true;
            }
        }
        return false;
    }

    public static String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String folded = value.toLowerCase(Locale.ROOT)
                .replace('\u2013', '-')
                .replace('\u2014', '-')
                .replace('\u2010', '-');
        return folded.replaceAll("[^a-z0-9]+", " ").trim().replaceAll("\\s+", " ");
    }

    static boolean isBranchTitle(String normalized) {
        if (normalized.contains("branch manager") || normalized.contains("senior branch manager")) {
            return true;
        }
        if (normalized.contains("district") || normalized.contains("regional")) {
            return false;
        }
        return normalized.startsWith("branch ")
                || normalized.equals("branch")
                || normalized.contains("csm")
                || normalized.contains("customer service manager")
                || normalized.contains("branch operations manager")
                || normalized.contains("branch head")
                || normalized.contains("branch leader");
    }

    static boolean isDistrictTitle(String normalized) {
        if (normalized.contains("district director") || normalized.contains("district business director")
                || normalized.contains("operational manager") || normalized.contains("district manager")
                || normalized.contains("district head") || normalized.contains("district leader")
                || normalized.contains("regional director") || normalized.contains("regional manager")) {
            return true;
        }
        return normalized.startsWith("district ") || normalized.equals("district");
    }

    static boolean isHeadOfficeTitle(String normalized) {
        if (isDistrictTitle(normalized) || isBranchTitle(normalized)) {
            return false;
        }
        if (normalized.contains("senior manager") && !normalized.contains("branch")) {
            return true;
        }
        if (normalized.contains("director") && !normalized.contains("district")) {
            return true;
        }
        return normalized.contains("department head")
                || normalized.contains("head of department")
                || normalized.contains("division head")
                || normalized.contains("unit head")
                || normalized.contains("chief officer")
                || normalized.contains("chief")
                || normalized.contains("vp")
                || normalized.contains("vice president")
                || normalized.contains("team leader")
                || normalized.contains("manager");
    }

    private static String groupKey(String dn) {
        if (!StringUtils.hasText(dn)) {
            return "";
        }
        String trimmed = dn.trim();
        if (trimmed.toUpperCase(Locale.ROOT).startsWith("CN=")) {
            int comma = trimmed.indexOf(',');
            return comma > 3 ? trimmed.substring(3, comma) : trimmed.substring(3);
        }
        return trimmed;
    }
}
