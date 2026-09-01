package com.dashenbank.cms.repository;

import com.dashenbank.cms.model.RcaAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RcaAuditLogRepository extends JpaRepository<RcaAuditLog, Long> {
    List<RcaAuditLog> findByRcaCaseId(Long rcaCaseId);
    List<RcaAuditLog> findByTicketId(String ticketId);
}
