package com.dashenbank.cms.security.ldap;

import com.dashenbank.cms.model.Role;

public record RoleResolution(Role role, String source, String matchedValue) {

    public static RoleResolution none() {
        return new RoleResolution(null, "NONE", null);
    }

    public boolean resolved() {
        return role != null;
    }
}
