package com.dashenbank.cms.notification.provider.datapower;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * In-process SMS meters. Names match the DataPower integration contract.
 */
@Component
public class SmsMetrics {

    public static final String SEND_SUCCESS = "sms.send.success";
    public static final String SEND_FAILURE = "sms.send.failure";
    public static final String SEND_RETRYABLE_FAILURE = "sms.send.retryable_failure";
    public static final String TOKEN_REFRESH_SUCCESS = "sms.token.refresh.success";
    public static final String TOKEN_REFRESH_FAILURE = "sms.token.refresh.failure";
    public static final String PROVIDER_LATENCY = "sms.provider.latency";
    public static final String QUEUE_PENDING = "sms.queue.pending";
    public static final String QUEUE_FAILED = "sms.queue.failed";

    static final String TAG_PROVIDER = "provider";

    private final MeterRegistry registry;

    public SmsMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    MeterRegistry registry() {
        return registry;
    }

    public void incrementSendSuccess(String provider) {
        increment(SEND_SUCCESS, provider);
    }

    public void incrementSendFailure(String provider) {
        increment(SEND_FAILURE, provider);
    }

    public void incrementSendRetryableFailure(String provider) {
        increment(SEND_RETRYABLE_FAILURE, provider);
    }

    public void incrementTokenRefreshSuccess(String provider) {
        increment(TOKEN_REFRESH_SUCCESS, provider);
    }

    public void incrementTokenRefreshFailure(String provider) {
        increment(TOKEN_REFRESH_FAILURE, provider);
    }

    public Timer.Sample startLatency() {
        return Timer.start(registry);
    }

    public void stopLatency(Timer.Sample sample, String provider) {
        if (sample == null) {
            return;
        }
        sample.stop(Timer.builder(PROVIDER_LATENCY).tag(TAG_PROVIDER, providerId(provider)).register(registry));
    }

    private void increment(String name, String provider) {
        registry.counter(name, TAG_PROVIDER, providerId(provider)).increment();
    }

    private static String providerId(String provider) {
        return provider == null || provider.isBlank() ? "unknown" : provider;
    }
}
