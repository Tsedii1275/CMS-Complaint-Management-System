package com.dashenbank.cms.repository;

import com.dashenbank.cms.model.NbeComplianceReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NbeComplianceReportRepository extends JpaRepository<NbeComplianceReport, Long> {

    Optional<NbeComplianceReport> findByTicketNumber(String ticketNumber);

    List<NbeComplianceReport> findByProcessInstanceId(String processInstanceId);

    boolean existsByTicketNumber(String ticketNumber);
}
