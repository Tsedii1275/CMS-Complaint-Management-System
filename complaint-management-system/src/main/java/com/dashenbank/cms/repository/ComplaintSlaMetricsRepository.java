package com.dashenbank.cms.repository;

import com.dashenbank.cms.model.ComplaintSlaMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ComplaintSlaMetricsRepository
        extends JpaRepository<ComplaintSlaMetrics, Long>, JpaSpecificationExecutor<ComplaintSlaMetrics> {
    Optional<ComplaintSlaMetrics> findByProcessInstanceId(String processInstanceId);

    Optional<ComplaintSlaMetrics> findByComplaintId(String complaintId);

    Optional<ComplaintSlaMetrics> findByGeneralTicketId(String generalTicketId);

    Optional<ComplaintSlaMetrics> findByDbcTicketId(String dbcTicketId);
}
