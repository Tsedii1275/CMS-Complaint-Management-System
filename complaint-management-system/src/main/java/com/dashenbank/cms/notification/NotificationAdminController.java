package com.dashenbank.cms.notification;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Admin view of the notification delivery trail. Recipients are masked and
 * message bodies are not returned.
 */
@RestController
@RequestMapping("/api/admin/notifications")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class NotificationAdminController {

    private final NotificationRecordRepository repository;
    private final NotificationService notificationService;

    public NotificationAdminController(NotificationRecordRepository repository,
            NotificationService notificationService) {
        this.repository = repository;
        this.notificationService = notificationService;
    }

    public record NotificationView(Long id, String complaintId, String processInstanceId, String eventType,
            String channel, String provider, String language, String recipient, String subject, String status,
            int attempts, int maxAttempts, String failureReason, String providerMessageId,
            LocalDateTime createdAt, LocalDateTime lastAttemptAt, LocalDateTime nextAttemptAt,
            LocalDateTime sentAt) {

        static NotificationView of(NotificationRecord n) {
            return new NotificationView(n.getId(), n.getComplaintRef(), n.getProcessInstanceId(),
                    String.valueOf(n.getEventType()), String.valueOf(n.getChannel()), n.getProvider(),
                    n.getLanguage(), RecipientFormat.mask(n.getChannel(), n.getRecipient()), n.getSubject(),
                    String.valueOf(n.getStatus()), n.getAttempts(), n.getMaxAttempts(), n.getFailureReason(),
                    n.getProviderMessageId(), n.getCreatedAt(), n.getLastAttemptAt(), n.getNextAttemptAt(),
                    n.getSentAt());
        }
    }

    @GetMapping
    public ResponseEntity<Object> list(@RequestParam(required = false) String complaintId,
            @RequestParam(required = false) String status) {
        List<NotificationRecord> rows;
        if (complaintId != null && !complaintId.isBlank()) {
            rows = repository.findByComplaintRefOrderByIdAsc(complaintId.trim());
        } else if (status != null && !status.isBlank()) {
            NotificationStatus parsed;
            try {
                parsed = NotificationStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Unknown notification status"));
            }
            rows = repository.findTop100ByStatusOrderByIdDesc(parsed);
        } else {
            rows = repository.findTop100ByOrderByIdDesc();
        }
        return ResponseEntity.ok(rows.stream().map(NotificationView::of).toList());
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<Object> retry(@PathVariable Long id, Authentication authentication) {
        String requestedBy = authentication != null ? authentication.getName() : "admin";
        try {
            return notificationService.requeue(id, requestedBy)
                    .<ResponseEntity<Object>>map(n -> ResponseEntity.ok(NotificationView.of(n)))
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }
}
