package com.dashenbank.cms.notification;

import com.dashenbank.cms.notification.provider.LoggingEmailProvider;
import com.dashenbank.cms.notification.provider.LoggingSmsProvider;
import com.dashenbank.cms.notification.provider.NotificationProviderRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-23T11:00:00Z"), ZoneId.of("UTC"));

    private NotificationRecordRepository repository;
    private NotificationDispatcher dispatcher;
    private NotificationAuditRecorder auditRecorder;
    private NotificationProperties properties;
    private final AtomicLong ids = new AtomicLong();

    @BeforeEach
    void setUp() {
        repository = mock(NotificationRecordRepository.class);
        dispatcher = mock(NotificationDispatcher.class);
        auditRecorder = mock(NotificationAuditRecorder.class);
        properties = new NotificationProperties();
        when(repository.save(any(NotificationRecord.class))).thenAnswer(invocation -> {
            NotificationRecord row = invocation.getArgument(0);
            row.setId(ids.incrementAndGet());
            return row;
        });
    }

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private NotificationService service() {
        NotificationProviderRegistry registry = new NotificationProviderRegistry(
                List.of(new LoggingEmailProvider()), List.of(new LoggingSmsProvider()), properties);
        NotificationTemplateService templates = new NotificationTemplateService(new DefaultResourceLoader(), properties);
        return new NotificationService(repository, templates, registry, dispatcher, auditRecorder, properties, CLOCK);
    }

    private static NotificationRequest registration(String email, String phone) {
        return CustomerNotifications.registration("DBC-2026-000001", "pi-1", "Abebe", email, phone, "english",
                LocalDateTime.of(2026, 9, 23, 14, 0));
    }

    @Test
    void queuesOneRowPerChannelAndDispatchesImmediatelyOutsideTransaction() {
        NotificationQueueResult result = service().notifyCustomer(registration("abebe@dashenbanksc.com", "0912345678"));

        assertTrue(result.emailQueued());
        assertTrue(result.smsQueued());
        ArgumentCaptor<NotificationRecord> saved = ArgumentCaptor.forClass(NotificationRecord.class);
        verify(repository, times(2)).save(saved.capture());
        NotificationRecord email = saved.getAllValues().get(0);
        NotificationRecord sms = saved.getAllValues().get(1);
        assertEquals(NotificationStatus.PENDING, email.getStatus());
        assertEquals("Complaint Registration Confirmation", email.getSubject());
        assertEquals("COMPLAINT_REGISTERED:DBC-2026-000001:EMAIL", email.getIdempotencyKey());
        assertEquals("+251912345678", sms.getRecipient());
        assertEquals("log", sms.getProvider());
        assertEquals(5, sms.getMaxAttempts());
        verify(dispatcher).dispatchAsync(List.of(1L, 2L));
        verify(auditRecorder, never()).recordOutcome(any());
    }

    @Test
    void waitsForCommitInsideTransaction() {
        TransactionSynchronizationManager.initSynchronization();

        service().notifyCustomer(registration("abebe@dashenbanksc.com", null));

        verify(dispatcher, never()).dispatchAsync(any());
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        assertEquals(1, synchronizations.size());
        synchronizations.get(0).afterCommit();
        verify(dispatcher).dispatchAsync(List.of(1L));
    }

    @Test
    void disabledChannelIsRecordedAsSkippedAndAudited() {
        properties.getSms().setEnabled(false);

        NotificationQueueResult result = service().notifyCustomer(registration(null, "0912345678"));

        assertFalse(result.smsQueued());
        ArgumentCaptor<NotificationRecord> saved = ArgumentCaptor.forClass(NotificationRecord.class);
        verify(repository).save(saved.capture());
        assertEquals(NotificationStatus.SKIPPED, saved.getValue().getStatus());
        assertTrue(saved.getValue().getFailureReason().contains("NOTIFICATION_SMS_ENABLED"));
        verify(auditRecorder).recordOutcome(saved.getValue());
        verify(dispatcher, never()).dispatchAsync(any());
    }

    @Test
    void invalidPhoneIsSkipped() {
        service().notifyCustomer(registration(null, "12"));

        ArgumentCaptor<NotificationRecord> saved = ArgumentCaptor.forClass(NotificationRecord.class);
        verify(repository).save(saved.capture());
        assertEquals(NotificationStatus.SKIPPED, saved.getValue().getStatus());
        assertEquals("Invalid phone number", saved.getValue().getFailureReason());
    }

    @Test
    void duplicateEventIsNotQueuedTwice() {
        when(repository.existsByIdempotencyKey("COMPLAINT_REGISTERED:DBC-2026-000001:EMAIL")).thenReturn(true);

        NotificationQueueResult result = service().notifyCustomer(registration("abebe@dashenbanksc.com", null));

        assertFalse(result.emailQueued());
        verify(repository, never()).save(any());
        verify(dispatcher, never()).dispatchAsync(any());
    }

    @Test
    void requestWithoutContactsQueuesNothing() {
        NotificationQueueResult result = service().notifyCustomer(registration(" ", null));

        assertFalse(result.emailQueued());
        assertFalse(result.smsQueued());
        verify(repository, never()).save(any());
    }
}
