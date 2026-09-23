package com.dashenbank.cms.notification.provider;

/**
 * Outcome of one delivery attempt. {@code detail} is stored in
 * {@code notifications.failure_reason} and must never contain credentials.
 */
public record ProviderResult(Outcome outcome, String providerMessageId, String detail) {

    public enum Outcome {
        DELIVERED,
        /** Provider accepted the call but intentionally did not deliver (for example the log provider). */
        NOT_DELIVERED,
        RETRYABLE_FAILURE,
        PERMANENT_FAILURE
    }

    public static ProviderResult delivered(String providerMessageId) {
        return new ProviderResult(Outcome.DELIVERED, providerMessageId, null);
    }

    public static ProviderResult notDelivered(String detail) {
        return new ProviderResult(Outcome.NOT_DELIVERED, null, detail);
    }

    public static ProviderResult retryable(String detail) {
        return new ProviderResult(Outcome.RETRYABLE_FAILURE, null, detail);
    }

    public static ProviderResult permanent(String detail) {
        return new ProviderResult(Outcome.PERMANENT_FAILURE, null, detail);
    }
}
