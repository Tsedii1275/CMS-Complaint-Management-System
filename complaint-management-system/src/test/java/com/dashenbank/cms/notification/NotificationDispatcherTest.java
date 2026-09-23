package com.dashenbank.cms.notification;

import com.dashenbank.cms.notification.provider.EmailMessage;
import com.dashenbank.cms.notification.provider.EmailProvider;
import com.dashenbank.cms.notification.provider.LoggingSmsProvider;
import com.dashenbank.cms.notification.provider.NotificationProviderRegistry;
import com.dashenbank.cms.notification.provider.ProviderResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationDispatcherTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-23T11:00:00Z"), ZoneId.of("UTC"));
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    private NotificationRecordRepository repository;
    private NotificationAuditRecorder auditRecorder;
    private StubEmailProvider emailProvider;
    private NotificationDispatcher dispatcher;

    static final class StubEmailProvider implements EmailProvider {
        final AtomicReference<ProviderResult> next = new AtomicReference<>(ProviderResult.delivered("msg-1"));
        final AtomicInteger calls = new AtomicInteger();

        @Override
        public String id() {
            return "stub";
        }

        @Override
        public ProviderResult send(EmailMessage message) {
            calls.incrementAndGet();
            ProviderResult result = next.get();
            if (result == null) {
                throw new IllegalStateException("transport down");
            }
            return result;
        }
    }

    @BeforeEach
    void setUp() {
        repository = mock(NotificationRecordRepository.class);
        auditRecorder = mock(NotificationAuditRecorder.class);
        NotificationProperties properties = new NotificationProperties();
        properties.getEmail().setProvider("stub");
        properties.getEmail().setFrom("customercare@dashenbanksc.com");
        emailProvider = new StubEmailProvider();
        NotificationProviderRegistry registry = new NotificationProviderRegistry(
                List.of(emailProvider), List.of(new LoggingSmsProvider()), properties);
        dispatcher = new NotificationDispatcher(repository, registry, properties, auditRecorder, CLOCK);
        when(repository.save(any(NotificationRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        dispatcher.shutdown();
    }

    private NotificationRecord claimedEmail(int attemptsAfterClaim) {
        NotificationRecord row = new NotificationRecord();
        row.setId(7L);
        row.setComplaintRef("DBC-1");
        row.setEventType(NotificationEventType.COMPLAINT_REGISTERED);
        row.setChannel(NotificationChannel.EMAIL);
        row.setRecipient("abebe@dashenbanksc.com");
        row.setSubject("Subject");
        row.setContent("Body");
        row.setStatus(NotificationStatus.SENDING);
        row.setAttempts(attemptsAfterClaim);
        row.setMaxAttempts(3);
        when(repository.claim(eq(7L), eq(NotificationStatus.SENDING), any(), any())).thenReturn(1);
        when(repository.findById(7L)).thenReturn(Optional.of(row));
        return row;
    }

    @Test
    void deliveredMarksSentAndAudits() {
        NotificationRecord row = claimedEmail(1);

        dispatcher.dispatch(7L);

        assertEquals(NotificationStatus.SENT, row.getStatus());
        assertEquals("msg-1", row.getProviderMessageId());
        assertEquals(NOW, row.getSentAt());
        assertEquals("stub", row.getProvider());
        verify(auditRecorder).recordOutcome(row);
    }

    @Test
    void retryableFailureSchedulesBackoff() {
        NotificationRecord row = claimedEmail(1);
        emailProvider.next.set(ProviderResult.retryable("SMTP timeout"));

        dispatcher.dispatch(7L);

        assertEquals(NotificationStatus.RETRY, row.getStatus());
        assertEquals(NOW.plusSeconds(60), row.getNextAttemptAt());
        assertEquals("SMTP timeout", row.getFailureReason());
    }

    @Test
    void retryableFailureOnLastAttemptFails() {
        NotificationRecord row = claimedEmail(3);
        emailProvider.next.set(ProviderResult.retryable("SMTP timeout"));

        dispatcher.dispatch(7L);

        assertEquals(NotificationStatus.FAILED, row.getStatus());
        assertNull(row.getNextAttemptAt());
    }

    @Test
    void permanentFailureFailsImmediately() {
        NotificationRecord row = claimedEmail(1);
        emailProvider.next.set(ProviderResult.permanent("Recipient address rejected by SMTP server"));

        dispatcher.dispatch(7L);

        assertEquals(NotificationStatus.FAILED, row.getStatus());
    }

    @Test
    void nonDeliveringProviderIsRecordedAsSkipped() {
        NotificationRecord row = claimedEmail(1);
        emailProvider.next.set(ProviderResult.notDelivered("recorded only"));

        dispatcher.dispatch(7L);

        assertEquals(NotificationStatus.SKIPPED, row.getStatus());
    }

    @Test
    void providerExceptionIsTreatedAsRetryable() {
        NotificationRecord row = claimedEmail(1);
        emailProvider.next.set(null);

        dispatcher.dispatch(7L);

        assertEquals(NotificationStatus.RETRY, row.getStatus());
        assertEquals("Provider error: IllegalStateException", row.getFailureReason());
    }

    @Test
    void lostClaimDoesNotSend() {
        when(repository.claim(anyLong(), any(), any(), any())).thenReturn(0);

        dispatcher.dispatch(7L);

        assertEquals(0, emailProvider.calls.get());
        verify(repository, never()).save(any());
    }

    @Test
    void backoffGrowsExponentiallyAndIsCapped() {
        assertEquals(60, dispatcher.backoffSeconds(1));
        assertEquals(300, dispatcher.backoffSeconds(2));
        assertEquals(1500, dispatcher.backoffSeconds(3));
        assertEquals(7200, dispatcher.backoffSeconds(4));
        assertEquals(7200, dispatcher.backoffSeconds(10));
    }
}
