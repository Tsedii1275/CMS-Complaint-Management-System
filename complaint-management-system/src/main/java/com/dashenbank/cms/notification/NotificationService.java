package com.dashenbank.cms.notification;

import com.dashenbank.cms.notification.provider.NotificationProviderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Single entry point for customer notifications.
 *
 * <p>Each channel becomes one row in {@code notifications}. When called inside
 * a transaction (for example a Flowable delegate) the rows commit or roll back
 * with the business change and delivery starts only after commit, so a rolled
 * back complaint never notifies the customer. Delivery itself is asynchronous.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final int MAX_RECIPIENT_LENGTH = 255;
    private static final int MAX_SUBJECT_LENGTH = 255;

    private final NotificationRecordRepository repository;
    private final NotificationTemplateService templates;
    private final NotificationProviderRegistry providers;
    private final NotificationDispatcher dispatcher;
    private final NotificationAuditRecorder auditRecorder;
    private final NotificationProperties properties;
    private final Clock clock;

    @Autowired
    public NotificationService(NotificationRecordRepository repository, NotificationTemplateService templates,
            NotificationProviderRegistry providers, NotificationDispatcher dispatcher,
            NotificationAuditRecorder auditRecorder, NotificationProperties properties) {
        this(repository, templates, providers, dispatcher, auditRecorder, properties, Clock.systemDefaultZone());
    }

    @SuppressWarnings("java:S107")
    NotificationService(NotificationRecordRepository repository, NotificationTemplateService templates,
            NotificationProviderRegistry providers, NotificationDispatcher dispatcher,
            NotificationAuditRecorder auditRecorder, NotificationProperties properties, Clock clock) {
        this.repository = repository;
        this.templates = templates;
        this.providers = providers;
        this.dispatcher = dispatcher;
        this.auditRecorder = auditRecorder;
        this.properties = properties;
        this.clock = clock;
    }

    public NotificationQueueResult notifyCustomer(NotificationRequest request) {
        if (request == null || request.eventType() == null) {
            return NotificationQueueResult.NONE;
        }
        List<Long> dispatchable = new ArrayList<>();
        boolean emailQueued = ContactChannelPreference.emailRequested(request.preferredContactMethod())
                && StringUtils.hasText(request.email())
                && enqueue(request, NotificationChannel.EMAIL, request.email(), dispatchable);
        boolean smsQueued = ContactChannelPreference.smsRequested(request.preferredContactMethod())
                && StringUtils.hasText(request.phone())
                && enqueue(request, NotificationChannel.SMS, request.phone(), dispatchable);
        dispatchAfterCommit(dispatchable);
        return new NotificationQueueResult(emailQueued, smsQueued);
    }

    /**
     * Returns a FAILED or SKIPPED notification to the retry queue with a fresh
     * attempt budget. Rows without rendered content cannot be re-sent.
     */
    public Optional<NotificationRecord> requeue(Long notificationId, String requestedBy) {
        Optional<NotificationRecord> found = repository.findById(notificationId);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        NotificationRecord notification = found.get();
        boolean finalState = notification.getStatus() == NotificationStatus.FAILED
                || notification.getStatus() == NotificationStatus.SKIPPED;
        if (!finalState || !StringUtils.hasText(notification.getContent())) {
            throw new IllegalStateException("Only FAILED or SKIPPED notifications with content can be re-queued");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        notification.setStatus(NotificationStatus.RETRY);
        notification.setNextAttemptAt(now);
        notification.setUpdatedAt(now);
        notification.setMaxAttempts(notification.getAttempts() + Math.max(1, properties.getRetry().getMaxAttempts()));
        NotificationRecord saved = repository.save(notification);
        auditRecorder.recordRetryRequested(saved, requestedBy);
        dispatcher.dispatchAsync(List.of(saved.getId()));
        return Optional.of(saved);
    }

    private boolean enqueue(NotificationRequest request, NotificationChannel channel, String rawRecipient,
            List<Long> dispatchable) {
        String idempotencyKey = idempotencyKey(request, channel);
        if (idempotencyKey != null) {
            Optional<NotificationRecord> existing = repository.findFirstByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return reuseOrRevive(existing.get(), request, channel, rawRecipient, dispatchable);
            }
        }

        LocalDateTime now = LocalDateTime.now(clock);
        String language = NotificationTemplateService.resolveLanguage(request.preferredLanguage());
        Optional<String> recipient = normalize(channel, rawRecipient);

        NotificationRecord notification = new NotificationRecord();
        notification.setComplaintRef(request.complaintRef());
        notification.setProcessInstanceId(request.processInstanceId());
        notification.setEventType(request.eventType());
        notification.setChannel(channel);
        notification.setProvider(providers.providerId(channel));
        notification.setLanguage(language);
        notification.setRecipient(truncate(recipient.orElse(rawRecipient.trim()), MAX_RECIPIENT_LENGTH));
        notification.setIdempotencyKey(idempotencyKey);
        notification.setMaxAttempts(Math.max(1, properties.getRetry().getMaxAttempts()));
        notification.setCreatedAt(now);
        notification.setUpdatedAt(now);
        notification.setNextAttemptAt(now);

        NotificationStatus status = NotificationStatus.PENDING;
        String reason = skipReason(channel, recipient.isPresent());
        if (reason != null) {
            status = NotificationStatus.SKIPPED;
        }
        try {
            RenderedMessage rendered = templates.render(request.eventType(), channel, language, request.variables());
            notification.setSubject(truncate(rendered.subject(), MAX_SUBJECT_LENGTH));
            notification.setContent(rendered.body());
        } catch (RuntimeException e) {
            notification.setContent("");
            status = NotificationStatus.FAILED;
            reason = "Template rendering failed: " + e.getClass().getSimpleName();
        }
        notification.setStatus(status);
        notification.setFailureReason(reason);
        if (status != NotificationStatus.PENDING) {
            notification.setNextAttemptAt(null);
        }

        NotificationRecord saved = repository.save(notification);
        if (status == NotificationStatus.PENDING) {
            dispatchable.add(saved.getId());
            return true;
        }
        log.info("[{}] {} notification #{} for {} not queued: {}", channel, request.eventType(), saved.getId(),
                RecipientFormat.mask(channel, saved.getRecipient()), reason);
        auditRecorder.recordOutcome(saved);
        return false;
    }

    /**
     * A message already pending or sent covers this channel. A skipped or failed
     * row is tried again so a later acknowledgment step can still deliver it.
     */
    private boolean reuseOrRevive(NotificationRecord existing, NotificationRequest request, NotificationChannel channel,
            String rawRecipient, List<Long> dispatchable) {
        NotificationStatus status = existing.getStatus();
        if (status == NotificationStatus.PENDING || status == NotificationStatus.SENDING
                || status == NotificationStatus.RETRY || status == NotificationStatus.SENT) {
            log.debug("{} {} notification for {} is already {}.", channel, request.eventType(),
                    request.complaintRef(), status);
            return true;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        String language = NotificationTemplateService.resolveLanguage(request.preferredLanguage());
        Optional<String> recipient = normalize(channel, rawRecipient);
        existing.setRecipient(truncate(recipient.orElse(rawRecipient.trim()), MAX_RECIPIENT_LENGTH));
        existing.setUpdatedAt(now);
        existing.setLanguage(language);
        NotificationStatus next = NotificationStatus.PENDING;
        String reason = skipReason(channel, recipient.isPresent());
        if (reason != null) {
            next = NotificationStatus.SKIPPED;
        }
        try {
            RenderedMessage rendered = templates.render(request.eventType(), channel, language, request.variables());
            existing.setSubject(truncate(rendered.subject(), MAX_SUBJECT_LENGTH));
            existing.setContent(rendered.body());
        } catch (RuntimeException e) {
            existing.setContent("");
            next = NotificationStatus.FAILED;
            reason = "Template rendering failed: " + e.getClass().getSimpleName();
        }
        existing.setStatus(next);
        existing.setFailureReason(reason);
        existing.setNextAttemptAt(next == NotificationStatus.PENDING ? now : null);
        existing.setMaxAttempts(existing.getAttempts() + Math.max(1, properties.getRetry().getMaxAttempts()));
        NotificationRecord saved = repository.save(existing);
        if (next == NotificationStatus.PENDING) {
            dispatchable.add(saved.getId());
            return true;
        }
        auditRecorder.recordOutcome(saved);
        return false;
    }

    private String skipReason(NotificationChannel channel, boolean validRecipient) {
        if (!properties.isEnabled()) {
            return "Notifications are disabled (NOTIFICATION_ENABLED=false)";
        }
        if (channel == NotificationChannel.EMAIL && !properties.getEmail().isEnabled()) {
            return "Email notifications are disabled (NOTIFICATION_EMAIL_ENABLED=false)";
        }
        if (channel == NotificationChannel.SMS && !properties.getSms().isEnabled()) {
            return "SMS notifications are disabled (NOTIFICATION_SMS_ENABLED=false)";
        }
        if (!validRecipient) {
            return channel == NotificationChannel.EMAIL ? "Invalid email address" : "Invalid phone number";
        }
        return null;
    }

    private Optional<String> normalize(NotificationChannel channel, String rawRecipient) {
        return channel == NotificationChannel.EMAIL
                ? RecipientFormat.normalizeEmail(rawRecipient)
                : RecipientFormat.normalizePhone(rawRecipient, properties.getSms().getDefaultCountryCode());
    }

    private static String idempotencyKey(NotificationRequest request, NotificationChannel channel) {
        if (!StringUtils.hasText(request.complaintRef())) {
            return null;
        }
        String key = request.eventType() + ":" + request.complaintRef().trim() + ":" + channel
                + (StringUtils.hasText(request.occurrenceKey()) ? ":" + request.occurrenceKey().trim() : "");
        return truncate(key, 191);
    }

    private void dispatchAfterCommit(List<Long> ids) {
        if (ids.isEmpty()) {
            return;
        }
        List<Long> toDispatch = List.copyOf(ids);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatcher.dispatchAsync(toDispatch);
                }
            });
        } else {
            dispatcher.dispatchAsync(toDispatch);
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() > max ? value.substring(0, max) : value;
    }
}
