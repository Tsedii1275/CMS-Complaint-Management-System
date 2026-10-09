package com.dashenbank.cms.security.ldap;

import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Associates AD people to the selected Work Unit when office/OU/department
 * names differ only by Branch / IFB / Department suffixes.
 */
public final class AdOrgUnitMatcher {

    private static final Set<String> NOISE = Set.of(
            "dashen", "bank", "sc", "plc", "ou", "branch", "window", "department", "dept",
            "directorate", "division", "unit", "office", "hq", "head");

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
            if (token.isEmpty() || NOISE.contains(token)) {
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
