package com.dashenbank.cms.security;

import org.springframework.util.StringUtils;

/**
 * Masks email, phone, and account numbers for logs, audit, and admin views.
 */
public final class SensitiveDataMasker {

    private SensitiveDataMasker() {
    }

    public static String email(String value) {
        if (!StringUtils.hasText(value) || !value.contains("@")) {
            return maskGeneric(value);
        }
        String trimmed = value.trim();
        int at = trimmed.indexOf('@');
        String local = trimmed.substring(0, at);
        String domain = trimmed.substring(at);
        if (local.length() <= 4) {
            return local.charAt(0) + "****" + domain;
        }
        return local.substring(0, 4) + "****" + domain;
    }

    public static String phone(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String digits = value.replaceAll("\\D", "");
        if (digits.length() < 6) {
            return "****";
        }
        return digits.substring(0, 4) + "****" + digits.substring(digits.length() - 2);
    }

    public static String accountNumber(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String digits = value.replaceAll("\\D", "");
        if (digits.length() <= 4) {
            return "****";
        }
        return "****" + digits.substring(digits.length() - 4);
    }

    private static String maskGeneric(String value) {
        return value == null || value.isBlank() ? "" : "****";
    }
}
