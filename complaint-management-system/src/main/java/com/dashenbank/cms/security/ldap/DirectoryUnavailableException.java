package com.dashenbank.cms.security.ldap;

public class DirectoryUnavailableException extends RuntimeException {
    public DirectoryUnavailableException(String message) {
        super(message);
    }

    public DirectoryUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
