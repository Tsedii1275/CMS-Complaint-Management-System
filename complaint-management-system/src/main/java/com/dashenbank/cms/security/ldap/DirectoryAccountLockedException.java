package com.dashenbank.cms.security.ldap;

/** Active Directory itself reports the account as locked out (bind error data 775). */
public class DirectoryAccountLockedException extends RuntimeException {
    public DirectoryAccountLockedException(String message) {
        super(message);
    }
}
