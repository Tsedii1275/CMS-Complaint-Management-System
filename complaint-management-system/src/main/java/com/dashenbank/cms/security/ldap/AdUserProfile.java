package com.dashenbank.cms.security.ldap;

import java.util.List;

public record AdUserProfile(
        String samAccountName,
        String objectGuid,
        String displayName,
        String email,
        String title,
        boolean enabled,
        List<String> memberOf,
        String department,
        String distinguishedName,
        String office) {

    public AdUserProfile {
        memberOf = memberOf == null ? List.of() : List.copyOf(memberOf);
    }

    public AdUserProfile(String samAccountName, String objectGuid, String displayName, String email, String title,
            boolean enabled, List<String> memberOf) {
        this(samAccountName, objectGuid, displayName, email, title, enabled, memberOf, null, null, null);
    }
}
