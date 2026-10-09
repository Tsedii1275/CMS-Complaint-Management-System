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

    /**
     * Branch assignment destination: conventional/IFB name, or a leaf OU sitting
     * under a District (Dashen often names those OUs {@code Bole} not {@code Bole Branch}).
     */
    public static boolean isAssignableBranch(String name, String distinguishedName) {
        if (looksLikeBranchUnit(name)) {
            return true;
        }
        return isDistrictChildBranch(name, distinguishedName);
    }

    public static boolean isDistrictChildBranch(String name, String distinguishedName) {
        String lower = fold(name);
        if (!isPlausibleBranchLocation(lower)) {
            return false;
        }
        if (!StringUtils.hasText(distinguishedName)) {
            return false;
        }
        String dn = distinguishedName.toLowerCase(Locale.ROOT);
        return (dn.contains("ou=") && (dn.contains("district") || dn.contains("region")))
                && !lower.contains("district") && !lower.contains("region");
    }

    public static boolean isPlausibleBranchLocation(String name) {
        String lower = fold(name);
        if (lower.isEmpty() || isContainerOu(lower)) {
            return false;
        }
        if (lower.contains("district") || lower.contains("region")) {
            return false;
        }
        if (isHeadOfficeIfbDepartment(lower)) {
            return false;
        }
        if ((lower.contains("department") || lower.contains("directorate") || lower.contains("division"))
                && !lower.contains("branch") && !isIfbName(lower)) {
            return false;
        }
        return true;
    }

    public static boolean looksLikeBranchUnit(String name) {
        String lower = fold(name);
        if (lower.isEmpty() || isContainerOu(lower)) {
            return false;
        }
        if (isHeadOfficeIfbDepartment(lower)) {
            return false;
        }
        if (lower.contains("head office") && !isIfbName(lower)) {
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
