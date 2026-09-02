package com.dashenbank.cms.security;

import java.util.regex.Pattern;

public final class SecurityPolicy {

    public static final int LOCKOUT_THRESHOLD = 6;
    public static final int LOCKOUT_DURATION_MINUTES = 30;
    public static final int SESSION_TIMEOUT_MINUTES = 15;
    public static final int PASSWORD_MIN_LENGTH = 12;
    public static final int PASSWORD_HISTORY_COUNT = 4;
    public static final int PASSWORD_EXPIRY_DAYS = 90;
    public static final int PASSWORD_WARNING_DAYS = 10;

    public static final int SESSION_TIMEOUT_MS = SESSION_TIMEOUT_MINUTES * 60 * 1000;

    public static final String PASSWORD_REQUIREMENTS_MESSAGE =
            "Password must be at least 12 characters and contain uppercase, lowercase, and numeric characters.";
    public static final String PASSWORD_HISTORY_MESSAGE =
            "You cannot reuse any of your last 4 passwords.";
    public static final String ACCOUNT_LOCKED_MESSAGE =
            "Your account has been locked due to multiple failed login attempts. "
                    + "Please try again after 30 minutes or contact the system administrator.";
    public static final String PASSWORD_EXPIRED_MESSAGE =
            "Your password has expired. Please change your password to continue.";
    public static final String SESSION_TIMEOUT_MESSAGE =
            "Your session has expired due to inactivity. Please login again.";

    static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{" + PASSWORD_MIN_LENGTH + ",}$");

    private SecurityPolicy() {
    }

    public static boolean meetsPasswordPolicy(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }
}
