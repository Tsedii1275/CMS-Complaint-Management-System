package com.dashenbank.cms.security.ldap;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Job-title matching for Work Unit assignment. Uses phrase catalogs (not a
 * single hardcoded title) plus optional LDAP-configured extras.
 */
public final class AdWorkUnitTitleMatcher {

    static final List<String> DEFAULT_BRANCH_TITLES = List.of(
            "branch manager",
            "senior branch manager",
            "acting branch manager",
            "customer service manager",
            "customer services manager",
            "senior customer service manager",
            "senior customer services manager",
            "acting customer service manager",
            "service manager",
            "senior service manager",
            "csm",
            "branch operations manager",
            "branch head",
            "branch leader",
            "ifb manager",
            "ifb head",
            "ifb leader",
            "ifb branch manager",
            "interest free banking manager",
            "interest free banking head");

    static final List<String> DEFAULT_HEAD_OFFICE_TITLES = List.of(
            "director",
            "department director",
            "division director",
            "senior manager",
            "acting senior manager",
            "manager",
            "department manager",
            "dept manager",
            "head of department",
            "department head",
            "division head",
            "head of division",
            "unit head",
            "chief officer",
            "vice president");

    static final List<String> DEFAULT_DISTRICT_TITLES = List.of(
            "district director",
            "district business director",
            "operational manager",
            "district manager",
            "district head",
            "district leader",
            "regional director",
            "regional manager");

    private AdWorkUnitTitleMatcher() {
    }

    public static boolean matchesTitle(String title, AdAssignmentScope scope) {
        return matchesTitle(title, scope, "", List.of());
    }

    public static boolean matchesTitle(String title, AdAssignmentScope scope, String unitName,
            Collection<String> extraPatterns) {
        String normalized = normalize(title);
        if (normalized.isEmpty()) {
            return false;
        }
        Collection<String> extras = extraPatterns == null ? List.of() : extraPatterns;
        return switch (scope) {
            case DISTRICT -> isDistrictTitle(normalized, extras);
            case BRANCH -> isBranchTitle(normalized, unitName, extras);
            case HEAD_OFFICE_DEPARTMENT -> isHeadOfficeTitle(normalized, extras);
        };
    }

    public static boolean matchesAnyWorkUnitTitle(String title) {
        String normalized = normalize(title);
        if (normalized.isEmpty()) {
            return false;
        }
        return isDistrictTitle(normalized)
                || isBranchTitle(normalized)
                || isHeadOfficeTitle(normalized);
    }

    public static boolean matchesGroup(List<String> memberOf, AdAssignmentScope scope) {
        return matchesGroup(memberOf, scope, List.of());
    }

    public static boolean matchesGroup(List<String> memberOf, AdAssignmentScope scope, Collection<String> extraPatterns) {
        if (memberOf == null || memberOf.isEmpty()) {
            return false;
        }
        for (String dn : memberOf) {
            if (matchesTitle(groupKey(dn), scope, "", extraPatterns)) {
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
        return isBranchTitle(normalized, "", List.of());
    }

    static boolean isBranchTitle(String normalized, String unitName, Collection<String> extras) {
        if (normalized.contains("district") || normalized.contains("regional")) {
            return false;
        }
        if (containsAnyPhrase(normalized, DEFAULT_BRANCH_TITLES) || containsAnyPhrase(normalized, extras)) {
            if (isBareServiceManager(normalized) && !isCustomerServiceTitle(normalized)
                    && StringUtils.hasText(unitName) && !AdBranchCatalog.looksLikeBranchUnit(unitName)) {
                return false;
            }
            return true;
        }
        if ((normalized.contains("interest free") || normalized.contains("interestfree"))
                && (normalized.contains("manager") || normalized.contains("head")
                        || normalized.contains("leader"))) {
            return true;
        }
        return normalized.startsWith("branch ") || normalized.equals("branch");
    }

    static boolean isDistrictTitle(String normalized) {
        return isDistrictTitle(normalized, List.of());
    }

    static boolean isDistrictTitle(String normalized, Collection<String> extras) {
        if (containsAnyPhrase(normalized, DEFAULT_DISTRICT_TITLES) || containsAnyPhrase(normalized, extras)) {
            return true;
        }
        return normalized.startsWith("district ") || normalized.equals("district");
    }

    static boolean isHeadOfficeTitle(String normalized) {
        return isHeadOfficeTitle(normalized, List.of());
    }

    static boolean isHeadOfficeTitle(String normalized, Collection<String> extras) {
        if (isDistrictTitle(normalized) || isExclusiveBranchLeadership(normalized)) {
            return false;
        }
        if (containsAnyPhrase(normalized, DEFAULT_HEAD_OFFICE_TITLES) || containsAnyPhrase(normalized, extras)) {
            return true;
        }
        return normalized.contains("chief") || normalized.equals("vp");
    }

    static boolean isExclusiveBranchLeadership(String normalized) {
        return containsPhrase(normalized, "branch manager")
                || containsPhrase(normalized, "customer service manager")
                || containsPhrase(normalized, "customer services manager")
                || containsPhrase(normalized, "csm")
                || containsPhrase(normalized, "branch head")
                || containsPhrase(normalized, "branch leader")
                || containsPhrase(normalized, "branch operations manager")
                || containsPhrase(normalized, "ifb manager")
                || containsPhrase(normalized, "ifb head")
                || containsPhrase(normalized, "ifb leader");
    }

    static boolean isBareServiceManager(String normalized) {
        return containsPhrase(normalized, "service manager") && !isCustomerServiceTitle(normalized);
    }

    static boolean isCustomerServiceTitle(String normalized) {
        return containsPhrase(normalized, "customer service")
                || containsPhrase(normalized, "customer services")
                || containsPhrase(normalized, "csm");
    }

    static boolean containsAnyPhrase(String normalized, Collection<String> patterns) {
        if (patterns == null) {
            return false;
        }
        for (String pattern : patterns) {
            String needle = normalize(pattern);
            if (!needle.isEmpty() && containsPhrase(normalized, needle)) {
                return true;
            }
        }
        return false;
    }

    static boolean containsPhrase(String haystack, String needle) {
        if (!StringUtils.hasText(haystack) || !StringUtils.hasText(needle)) {
            return false;
        }
        if (haystack.equals(needle)) {
            return true;
        }
        String padded = " " + haystack + " ";
        return padded.contains(" " + needle + " ");
    }

    static List<String> parseTitleList(String raw) {
        List<String> out = new ArrayList<>();
        if (!StringUtils.hasText(raw)) {
            return out;
        }
        for (String part : raw.split("[,;|]")) {
            String item = part.trim();
            if (!item.isEmpty()) {
                out.add(item);
            }
        }
        return out;
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
