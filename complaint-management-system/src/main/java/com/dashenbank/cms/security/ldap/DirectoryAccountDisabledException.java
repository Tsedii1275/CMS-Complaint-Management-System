package com.dashenbank.cms.security.ldap;

public class DirectoryAccountDisabledException extends RuntimeException {
    public DirectoryAccountDisabledException(String message) {
        super(message);
    }
}
