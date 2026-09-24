package com.dashenbank.cms.notification;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Recipient validation, normalization and log masking.
 */
public final class RecipientFormat {

    private static final Pattern EMAIL = Pattern.compile(
            "^[A-Za-z0-9._%+'-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$");
    private static final Pattern E164_DIGITS = Pattern.compile("[1-9]\\d{7,14}");
    private static final Pattern PHONE_SEPARATORS = Pattern.compile("[\\s().-]");

    private RecipientFormat() {
    }

    public static Optional<String> normalizeEmail(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String trimmed = raw.trim();
        return EMAIL.matcher(trimmed).matches() ? Optional.of(trimmed) : Optional.empty();
    }

    /**
     * Converts a local or international number to E.164. Numbers without an
     * international prefix are treated as national numbers of
     * {@code defaultCountryCode} (a leading trunk 0 is dropped).
     */
    public static Optional<String> normalizePhone(String raw, String defaultCountryCode) {
        if (raw == null) {
            return Optional.empty();
        }
        String compact = PHONE_SEPARATORS.matcher(raw.trim()).replaceAll("");
        String countryCode = defaultCountryCode == null ? "" : defaultCountryCode.replaceAll("\\D", "");
        String digits;
        if (compact.startsWith("+")) {
            digits = compact.substring(1);
        } else if (compact.startsWith("00")) {
            digits = compact.substring(2);
        } else if (countryCode.isEmpty()) {
            return Optional.empty();
        } else if (compact.startsWith(countryCode) && compact.length() > countryCode.length() + 8) {
            digits = compact;
        } else if (compact.startsWith("0")) {
            digits = countryCode + compact.substring(1);
        } else {
            digits = countryCode + compact;
        }
        if (!countryCode.isEmpty() && digits.startsWith(countryCode + "0")) {
            digits = countryCode + digits.substring(countryCode.length() + 1);
        }
        return E164_DIGITS.matcher(digits).matches() ? Optional.of("+" + digits) : Optional.empty();
    }

    public static String mask(NotificationChannel channel, String recipient) {
        return channel == NotificationChannel.EMAIL ? maskEmail(recipient) : maskPhone(recipient);
    }

    public static String maskEmail(String email) {
        String masked = com.dashenbank.cms.security.SensitiveDataMasker.email(email);
        return masked.isBlank() ? "***" : masked;
    }

    public static String maskPhone(String phone) {
        return com.dashenbank.cms.security.SensitiveDataMasker.phone(phone);
    }
}
