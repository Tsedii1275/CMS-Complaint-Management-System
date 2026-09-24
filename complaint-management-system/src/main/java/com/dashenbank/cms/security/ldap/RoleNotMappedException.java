package com.dashenbank.cms.security.ldap;

public class RoleNotMappedException extends RuntimeException {
    public RoleNotMappedException(String message) {
        super(message);
    }
}
