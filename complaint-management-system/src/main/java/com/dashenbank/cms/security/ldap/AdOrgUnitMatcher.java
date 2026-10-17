package com.dashenbank.cms.security.ldap;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Associates AD people to the selected Work Unit when office/OU/department
 * names differ only by Branch / IFB / Department suffixes.
 */
public final class AdOrgUnitMatcher {

    private static final Set<String> NOISE = Set.of(
            "dashen", "bank", "sc", "plc", "ou", "branch", "window", "department", "dept",
            "directorate", "division", "unit", "office", "hq", "head");

    /** Words that are not part of a branch place name when matching an office to a folder. */
    private static final Set<String> PLACE_NOISE = Set.of("dashen", "bank", "branch", "window");

    /** Three-digit codes (075), letter-prefixed codes (F25), or a single trailing digit. Two digits stay, so "04" keeps Ayer Tena 04 separate. */
    private static final Pattern BRANCH_CODE = Pattern.compile("(?:\\d{3,}|[a-z]\\d{2,}|\\d)");

    private AdOrgUnitMatcher() {
    }

    public static boolean sameUnit(String left, String right) {
        if (!StringUtils.hasText(left) || !StringUtils.hasText(right)) {
            return false;
        }
        if (left.trim().equalsIgnoreCase(right.trim())) {
            return true;
        }
        Canonical a = canonicalize(left);
        Canonical b = canonicalize(right);
        if (a.core().isEmpty() || b.core().isEmpty()) {
            return false;
        }
        return a.ifb() == b.ifb() && a.core().equals(b.core());
    }

    /**
     * Office or department versus the selected branch folder. Ignores a leading
     * "Dashen" and a trailing branch code, and keeps numbered and IFB neighbours apart.
     */
    public static boolean sameBranchPlace(String left, String right) {
        String a = branchPlaceKey(left);
        String b = branchPlaceKey(right);
        return !a.isEmpty() && a.equals(b);
    }

    static String branchPlaceKey(String name) {
        if (!StringUtils.hasText(name)) {
            return "";
        }
        String folded = AdWorkUnitTitleMatcher.normalize(name);
        boolean ifb = AdBranchCatalog.isIfbName(folded) || folded.contains("fifb");
        folded = folded.replace("interest free banking", " ")
                .replace("interestfree", " ")
                .replace("fifb", " ")
                .replace(" ifb ", " ");
        if (folded.startsWith("ifb ")) {
            folded = folded.substring(4);
        }
        if (folded.endsWith(" ifb")) {
            folded = folded.substring(0, folded.length() - 4);
        }
        List<String> tokens = new ArrayList<>();
        for (String token : folded.split(" ")) {
            if (token.isEmpty() || PLACE_NOISE.contains(token) || "ifb".equals(token) || "fifb".equals(token)) {
                continue;
            }
            tokens.add(token);
        }
        if (!tokens.isEmpty() && BRANCH_CODE.matcher(tokens.get(tokens.size() - 1)).matches()) {
            tokens.remove(tokens.size() - 1);
        }
        if (tokens.isEmpty()) {
            return "";
        }
        return (ifb ? "ifb:" : "br:") + String.join("", tokens);
    }

    static Canonical canonicalize(String name) {
        String folded = stripOrgPrefix(AdWorkUnitTitleMatcher.normalize(name));
        boolean ifb = AdBranchCatalog.isIfbName(folded);
        folded = folded.replace("interest free banking", " ")
                .replace("interestfree", " ")
                .replace(" ifb ", " ");
        if (folded.startsWith("ifb ")) {
            folded = folded.substring(4);
        }
        if (folded.endsWith(" ifb")) {
            folded = folded.substring(0, folded.length() - 4);
        }
        StringBuilder core = new StringBuilder();
        for (String token : folded.split(" ")) {
            if (token.isEmpty() || NOISE.contains(token) || token.chars().allMatch(Character::isDigit)) {
                continue;
            }
            if (!core.isEmpty()) {
                core.append(' ');
            }
            core.append(token);
        }
        return new Canonical(core.toString().trim().toLowerCase(Locale.ROOT), ifb);
    }

    static String stripOrgPrefix(String folded) {
        String current = folded == null ? "" : folded.trim();
        boolean stripped = true;
        while (stripped && !current.isEmpty()) {
            stripped = false;
            for (String prefix : List.of("dashen bank ", "dashens bank ", "dashen s bank ", "dashenbank ",
                    "dashen ", "dashens ")) {
                if (current.startsWith(prefix)) {
                    current = current.substring(prefix.length()).trim();
                    stripped = true;
                    break;
                }
            }
        }
        return current;
    }

    record Canonical(String core, boolean ifb) {
    }
}
