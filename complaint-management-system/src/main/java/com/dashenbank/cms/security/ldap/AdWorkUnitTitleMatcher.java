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
        return normalized.equals("branch manager")
                || normalized.startsWith("branch manager ")
                || normalized.equals("senior branch manager")
                || normalized.startsWith("senior branch manager ");
    }

    static boolean isDistrictTitle(String normalized) {
        return normalized.equals("district director")
                || normalized.startsWith("district director ")
                || normalized.equals("district business director")
                || normalized.startsWith("district business director ")
                || normalized.equals("operational manager")
                || normalized.startsWith("operational manager ");
    }

    static boolean isHeadOfficeTitle(String normalized) {
        if (isDistrictTitle(normalized) || isBranchTitle(normalized)) {
            return false;
        }
        if (normalized.equals("senior manager") || (normalized.startsWith("senior manager ")
                && !normalized.contains("branch"))) {
            return true;
        }
        if (normalized.equals("director") || normalized.startsWith("director ")) {
            return !normalized.contains("district");
        }
        return false;
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
