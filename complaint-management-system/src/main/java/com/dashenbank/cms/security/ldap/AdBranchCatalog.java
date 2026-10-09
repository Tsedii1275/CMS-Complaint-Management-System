package com.dashenbank.cms.security.ldap;

import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * Classifies Dashen location names for Work Unit Assignment. Conventional
 * branches and IFB (Interest Free Banking) windows are both valid destinations.
 * Head-office IFB departments are not branches.
 */
public final class AdBranchCatalog {

    private AdBranchCatalog() {
    }

    public static boolean looksLikeBranchUnit(String name) {
        String lower = fold(name);
        if (lower.isEmpty() || isContainerOu(lower)) {
            return false;
        }
        if (isHeadOfficeIfbDepartment(lower)) {
            return false;
        }
        if (isIfbBranchWindow(lower)) {
            return true;
        }
        if (lower.contains("district") || lower.contains("region")) {
            return false;
        }
        if ((lower.contains("department") || lower.contains("directorate") || lower.contains("division"))
                && !lower.contains("branch")) {
            return false;
        }
        return lower.contains("branch");
    }

    public static boolean isIfbBranchWindow(String name) {
        String lower = fold(name);
        return isIfbName(lower) && !isHeadOfficeIfbDepartment(lower);
    }

    public static boolean isHeadOfficeIfbDepartment(String name) {
        String lower = fold(name);
        if (!isIfbName(lower)) {
            return false;
        }
        boolean hoShape = lower.contains("department") || lower.contains("directorate")
                || lower.contains("division") || lower.contains("head office");
        return hoShape && !lower.contains("branch") && !lower.contains("window");
    }

    static boolean isIfbName(String lower) {
        return lower.contains("ifb") || lower.contains("interest free") || lower.contains("interestfree");
    }

    static boolean isContainerOu(String lower) {
        return lower.equals("users") || lower.equals("builtin") || lower.equals("domain controllers")
                || lower.equals("computers") || lower.equals("groups") || lower.equals("service user")
                || lower.equals("dashen bank") || lower.equals("dashen head office");
    }

    private static String fold(String name) {
        if (!StringUtils.hasText(name)) {
            return "";
        }
        return name.trim().toLowerCase(Locale.ROOT).replace('-', ' ').replaceAll("\\s+", " ");
    }
}
