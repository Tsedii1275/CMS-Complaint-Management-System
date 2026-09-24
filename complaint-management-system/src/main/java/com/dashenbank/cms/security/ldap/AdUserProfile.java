package com.dashenbank.cms.security.ldap;

import java.util.List;

public record AdUserProfile(
        String samAccountName,
        String objectGuid,
        String displayName,
        String email,
        String title,
        boolean enabled,
        List<String> memberOf) {

    public AdUserProfile {
        memberOf = memberOf == null ? List.of() : List.copyOf(memberOf);
    }
}
