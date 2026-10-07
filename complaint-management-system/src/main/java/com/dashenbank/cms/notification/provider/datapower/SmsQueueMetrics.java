package com.dashenbank.cms.notification.provider.datapower;

import com.dashenbank.cms.notification.NotificationChannel;
import com.dashenbank.cms.notification.NotificationProperties;
import com.dashenbank.cms.notification.NotificationRecordRepository;
import com.dashenbank.cms.notification.NotificationStatus;
import io.micrometer.core.instrument.Gauge;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/**
 * Gauges for SMS outbox depth. Pending includes rows the dispatcher still owns.
 */
@Component
public class SmsQueueMetrics {

    private static final Set<NotificationStatus> PENDING = EnumSet.of(
            NotificationStatus.PENDING, NotificationStatus.RETRY, NotificationStatus.SENDING);

    private final NotificationRecordRepository repository;
    private final NotificationProperties properties;
    private final SmsMetrics metrics;

    public SmsQueueMetrics(NotificationRecordRepository repository, NotificationProperties properties,
            SmsMetrics metrics) {
        this.repository = repository;
        this.properties = properties;
        this.metrics = metrics;
    }

    @PostConstruct
    void register() {
        String provider = properties.getSms().getProvider();
        Gauge.builder(SmsMetrics.QUEUE_PENDING, this, SmsQueueMetrics::pendingCount)
                .tag(SmsMetrics.TAG_PROVIDER, provider == null ? "log" : provider)
                .register(metrics.registry());
        Gauge.builder(SmsMetrics.QUEUE_FAILED, this, SmsQueueMetrics::failedCount)
                .tag(SmsMetrics.TAG_PROVIDER, provider == null ? "log" : provider)
                .register(metrics.registry());
    }

    private double pendingCount() {
        try {
            return repository.countByChannelAndStatusIn(NotificationChannel.SMS, PENDING);
        } catch (RuntimeException e) {
            return 0;
        }
    }

    private double failedCount() {
        try {
            return repository.countByChannelAndStatus(NotificationChannel.SMS, NotificationStatus.FAILED);
        } catch (RuntimeException e) {
            return 0;
        }
    }
}
