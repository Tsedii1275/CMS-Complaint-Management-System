package com.dashenbank.cms.notification;

/**
 * The customer's preferred acknowledgment channel. A blank or unknown value
 * keeps the previous behaviour and uses every contact that was provided.
 */
public final class ContactChannelPreference {

    private ContactChannelPreference() {
    }

    public static boolean emailRequested(String method) {
        String normalized = normalize(method);
        return normalized == null || "BOTH".equals(normalized) || "EMAIL".equals(normalized);
    }

    public static boolean smsRequested(String method) {
        String normalized = normalize(method);
        return normalized == null || "BOTH".equals(normalized) || "SMS".equals(normalized);
    }

    /** True only when every channel the customer asked for was queued or already on its way. */
    public static boolean covered(String method, boolean emailQueued, boolean smsQueued) {
        return (!emailRequested(method) || emailQueued) && (!smsRequested(method) || smsQueued);
    }

    private static String normalize(String method) {
        if (method == null || method.isBlank()) {
            return null;
        }
        return method.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
