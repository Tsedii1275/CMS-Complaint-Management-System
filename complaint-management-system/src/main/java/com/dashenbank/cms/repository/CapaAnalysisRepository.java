package com.dashenbank.cms.repository;

import com.dashenbank.cms.model.CapaAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CapaAnalysisRepository extends JpaRepository<CapaAnalysis, Long> {

    Optional<CapaAnalysis> findByComplaintNatureIgnoreCase(String complaintNature);
}
