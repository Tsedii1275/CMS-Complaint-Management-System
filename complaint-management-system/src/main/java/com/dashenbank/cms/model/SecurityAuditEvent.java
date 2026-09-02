package com.dashenbank.cms.model;

public enum SecurityAuditEvent {
    FAILED_LOGIN,
    ACCOUNT_LOCKED,
    ACCOUNT_UNLOCKED,
    PASSWORD_CHANGED,
    PASSWORD_EXPIRED,
    PASSWORD_RESET,
    SESSION_TIMEOUT
}
