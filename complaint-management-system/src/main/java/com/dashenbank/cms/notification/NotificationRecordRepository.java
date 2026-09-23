package com.dashenbank.cms.notification;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface NotificationRecordRepository extends JpaRepository<NotificationRecord, Long> {

    boolean existsByIdempotencyKey(String idempotencyKey);

    List<NotificationRecord> findByComplaintRefOrderByIdAsc(String complaintRef);

    List<NotificationRecord> findTop100ByStatusOrderByIdDesc(NotificationStatus status);

    List<NotificationRecord> findTop100ByOrderByIdDesc();

    /**
     * Atomically moves a due row to SENDING so that only one worker (or one
     * application instance) delivers it. Returns 1 when this caller won the claim.
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE NotificationRecord n SET n.status = :sending, n.attempts = n.attempts + 1, "
            + "n.lastAttemptAt = :now, n.updatedAt = :now "
            + "WHERE n.id = :id AND n.status IN :claimable "
            + "AND (n.nextAttemptAt IS NULL OR n.nextAttemptAt <= :now)")
    int claim(@Param("id") Long id,
            @Param("sending") NotificationStatus sending,
            @Param("claimable") Collection<NotificationStatus> claimable,
            @Param("now") LocalDateTime now);

    @Query("SELECT n.id FROM NotificationRecord n WHERE n.status IN :statuses "
            + "AND (n.nextAttemptAt IS NULL OR n.nextAttemptAt <= :now) ORDER BY n.id")
    List<Long> findDueIds(@Param("statuses") Collection<NotificationStatus> statuses,
            @Param("now") LocalDateTime now,
            Pageable page);

    /** Returns rows left in SENDING by a crashed worker to the retry queue. */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE NotificationRecord n SET n.status = :retry, n.nextAttemptAt = :now, n.updatedAt = :now "
            + "WHERE n.status = :sending AND n.lastAttemptAt < :staleBefore")
    int releaseStale(@Param("sending") NotificationStatus sending,
            @Param("retry") NotificationStatus retry,
            @Param("staleBefore") LocalDateTime staleBefore,
            @Param("now") LocalDateTime now);
}
