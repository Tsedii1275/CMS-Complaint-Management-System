package com.dashenbank.cms.notification.provider.datapower;

import com.dashenbank.cms.notification.NotificationChannel;
import com.dashenbank.cms.notification.NotificationProperties;
import com.dashenbank.cms.notification.NotificationRecordRepository;
import com.dashenbank.cms.notification.NotificationStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SmsQueueMetricsTest {

    @Test
    void exposesPendingAndFailedGauges() {
        NotificationRecordRepository repository = mock(NotificationRecordRepository.class);
        when(repository.countByChannelAndStatusIn(eq(NotificationChannel.SMS), any(Collection.class))).thenReturn(4L);
        when(repository.countByChannelAndStatus(NotificationChannel.SMS, NotificationStatus.FAILED)).thenReturn(2L);
        NotificationProperties properties = new NotificationProperties();
        properties.getSms().setProvider(DataPowerSmsProvider.ID);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        SmsQueueMetrics gauges = new SmsQueueMetrics(repository, properties, new SmsMetrics(registry));
        gauges.register();

        assertEquals(4.0, registry.get(SmsMetrics.QUEUE_PENDING).tag("provider", "datapower").gauge().value());
        assertEquals(2.0, registry.get(SmsMetrics.QUEUE_FAILED).tag("provider", "datapower").gauge().value());
    }
}
