package com.dashenbank.cms.repository;

import com.dashenbank.cms.model.SecurityAuditEvent;
import com.dashenbank.cms.model.SecurityAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SecurityAuditLogRepository extends JpaRepository<SecurityAuditLog, Long> {

    List<SecurityAuditLog> findTop200ByOrderByCreatedAtDesc();

    List<SecurityAuditLog> findTop100ByEventTypeInOrderByCreatedAtDesc(List<SecurityAuditEvent> types);

    interface EventSummary {
        SecurityAuditEvent getEventType();

        long getEventCount();

        LocalDateTime getLastOccurrence();
    }

    @Query("""
            select e.eventType as eventType, count(e) as eventCount, max(e.createdAt) as lastOccurrence
            from SecurityAuditLog e
            group by e.eventType
            order by max(e.createdAt) desc
            """)
    List<EventSummary> summarizeByEvent();
}
