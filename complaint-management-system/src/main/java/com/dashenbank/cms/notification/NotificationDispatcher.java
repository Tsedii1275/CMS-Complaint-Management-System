package com.dashenbank.cms.notification;

import com.dashenbank.cms.notification.provider.EmailMessage;
import com.dashenbank.cms.notification.provider.EmailProvider;
import com.dashenbank.cms.notification.provider.NotificationProviderRegistry;
import com.dashenbank.cms.notification.provider.ProviderResult;
import com.dashenbank.cms.notification.provider.SmsMessage;
import com.dashenbank.cms.notification.provider.SmsProvider;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Delivers queued notifications on a dedicated bounded thread pool, applies
 * retry with exponential backoff, and records the real outcome. A scheduled
 * poller picks up retries, rows missed after a restart, and rows whose
 * immediate dispatch was rejected because the queue was full.
 */
@Component
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);
    private static final Set<NotificationStatus> CLAIMABLE = EnumSet.of(NotificationStatus.PENDING,
            NotificationStatus.RETRY);
    private static final int MAX_REASON_LENGTH = 500;

    private final NotificationRecordRepository repository;
    private final NotificationProviderRegistry providers;
    private final NotificationProperties properties;
    private final NotificationAuditRecorder auditRecorder;
    private final Clock clock;
    private final ThreadPoolTaskExecutor executor;

    @Autowired
    public NotificationDispatcher(NotificationRecordRepository repository, NotificationProviderRegistry providers,
            NotificationProperties properties, NotificationAuditRecorder auditRecorder) {
        this(repository, providers, properties, auditRecorder, Clock.systemDefaultZone());
    }

    NotificationDispatcher(NotificationRecordRepository repository, NotificationProviderRegistry providers,
            NotificationProperties properties, NotificationAuditRecorder auditRecorder, Clock clock) {
        this.repository = repository;
        this.providers = providers;
        this.properties = properties;
        this.auditRecorder = auditRecorder;
        this.clock = clock;
        this.executor = createExecutor(properties.getDispatcher());
    }

    public void dispatchAsync(Collection<Long> notificationIds) {
        for (Long id : notificationIds) {
            try {
                executor.execute(() -> dispatch(id));
            } catch (TaskRejectedException e) {
                log.warn("Notification #{} left for the retry poller: dispatch queue is full.", id);
            }
        }
    }

    public void dispatch(Long notificationId) {
        try {
            LocalDateTime now = LocalDateTime.now(clock);
            if (repository.claim(notificationId, NotificationStatus.SENDING, CLAIMABLE, now) == 0) {
                return;
            }
            NotificationRecord notification = repository.findById(notificationId).orElse(null);
            if (notification == null) {
                return;
            }
            ProviderResult result = deliver(notification);
            applyResult(notification, result != null ? result : ProviderResult.retryable("Provider returned no result"));
        } catch (RuntimeException e) {
            log.error("Notification #{} dispatch error: {}", notificationId, e.getClass().getSimpleName());
        }
    }

    @Scheduled(fixedDelayString = "${notification.dispatcher.poll-interval-ms:30000}",
            initialDelayString = "${notification.dispatcher.initial-delay-ms:30000}")
    public void dispatchDue() {
        NotificationProperties.Dispatcher config = properties.getDispatcher();
        if (!config.isEnabled()) {
            return;
        }
        try {
            LocalDateTime now = LocalDateTime.now(clock);
            int released = repository.releaseStale(NotificationStatus.SENDING, NotificationStatus.RETRY,
                    now.minusSeconds(Math.max(30, config.getStaleAfterSeconds())), now);
            if (released > 0) {
                log.warn("{} notification(s) stuck in SENDING were returned to the retry queue.", released);
            }
            List<Long> due = repository.findDueIds(CLAIMABLE, now, PageRequest.of(0, Math.max(1, config.getBatchSize())));
            dispatchAsync(due);
        } catch (RuntimeException e) {
            log.error("Notification poll failed: {}", e.getClass().getSimpleName());
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }

    private ProviderResult deliver(NotificationRecord notification) {
        try {
            if (notification.getChannel() == NotificationChannel.EMAIL) {
                if (!channelEnabled(properties.getEmail().isEnabled())) {
                    return ProviderResult.notDelivered("Email notifications are disabled");
                }
                EmailProvider provider = providers.emailProvider();
                notification.setProvider(provider.id());
                NotificationProperties.Email email = properties.getEmail();
                return provider.send(new EmailMessage(notification.getRecipient(), email.getFrom(),
                        email.getFromName(), email.getReplyTo(), notification.getSubject(), notification.getContent()));
            }
            if (!channelEnabled(properties.getSms().isEnabled())) {
                return ProviderResult.notDelivered("SMS notifications are disabled");
            }
            SmsProvider provider = providers.smsProvider();
            notification.setProvider(provider.id());
            return provider.send(new SmsMessage(notification.getRecipient(), properties.getSms().getSenderId(),
                    notification.getContent()));
        } catch (RuntimeException e) {
            return ProviderResult.retryable("Provider error: " + e.getClass().getSimpleName());
        }
    }

    private boolean channelEnabled(boolean channelSwitch) {
        return properties.isEnabled() && channelSwitch;
    }

    private void applyResult(NotificationRecord notification, ProviderResult result) {
        LocalDateTime now = LocalDateTime.now(clock);
        notification.setUpdatedAt(now);
        String masked = RecipientFormat.mask(notification.getChannel(), notification.getRecipient());
        switch (result.outcome()) {
            case DELIVERED -> {
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(now);
                notification.setNextAttemptAt(null);
                notification.setFailureReason(null);
                notification.setProviderMessageId(result.providerMessageId());
                log.info("[{}] Notification #{} delivered to {} via {}.", notification.getChannel(),
                        notification.getId(), masked, notification.getProvider());
            }
            case NOT_DELIVERED -> {
                notification.setStatus(NotificationStatus.SKIPPED);
                notification.setNextAttemptAt(null);
                notification.setFailureReason(truncate(result.detail()));
            }
            case PERMANENT_FAILURE -> {
                notification.setStatus(NotificationStatus.FAILED);
                notification.setNextAttemptAt(null);
                notification.setFailureReason(truncate(result.detail()));
                log.warn("[{}] Notification #{} to {} failed permanently via {}.", notification.getChannel(),
                        notification.getId(), masked, notification.getProvider());
            }
            case RETRYABLE_FAILURE -> applyRetryableFailure(notification, result, now, masked);
        }
        repository.save(notification);
        auditRecorder.recordOutcome(notification);
    }

    private void applyRetryableFailure(NotificationRecord notification, ProviderResult result, LocalDateTime now,
            String masked) {
        notification.setFailureReason(truncate(result.detail()));
        if (notification.getAttempts() >= notification.getMaxAttempts()) {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setNextAttemptAt(null);
            log.warn("[{}] Notification #{} to {} failed after {} attempt(s) via {}; giving up.",
                    notification.getChannel(), notification.getId(), masked, notification.getAttempts(),
                    notification.getProvider());
            return;
        }
        long delaySeconds = backoffSeconds(notification.getAttempts());
        notification.setStatus(NotificationStatus.RETRY);
        notification.setNextAttemptAt(now.plusSeconds(delaySeconds));
        log.warn("[{}] Notification #{} to {} attempt {} failed via {}; retrying in {}s.",
                notification.getChannel(), notification.getId(), masked, notification.getAttempts(),
                notification.getProvider(), delaySeconds);
    }

    long backoffSeconds(int attemptsSoFar) {
        NotificationProperties.Retry retry = properties.getRetry();
        double delay = Math.max(1, retry.getInitialDelaySeconds())
                * Math.pow(Math.max(1.0, retry.getMultiplier()), Math.max(0, attemptsSoFar - 1));
        return (long) Math.min(delay, Math.max(1, retry.getMaxDelaySeconds()));
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > MAX_REASON_LENGTH ? value.substring(0, MAX_REASON_LENGTH) : value;
    }

    private static ThreadPoolTaskExecutor createExecutor(NotificationProperties.Dispatcher config) {
        int core = Math.max(1, config.getCorePoolSize());
        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        pool.setThreadNamePrefix("notification-");
        pool.setCorePoolSize(core);
        pool.setMaxPoolSize(Math.max(core, config.getMaxPoolSize()));
        pool.setQueueCapacity(Math.max(0, config.getQueueCapacity()));
        pool.setWaitForTasksToCompleteOnShutdown(true);
        pool.setAwaitTerminationSeconds(20);
        pool.initialize();
        return pool;
    }
}
