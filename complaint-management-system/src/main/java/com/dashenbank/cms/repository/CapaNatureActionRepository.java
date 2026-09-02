package com.dashenbank.cms.repository;

import com.dashenbank.cms.model.CapaNatureAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CapaNatureActionRepository extends JpaRepository<CapaNatureAction, Long> {

    Optional<CapaNatureAction> findByCapaAnalysis_IdAndActionType(Long capaAnalysisId, String actionType);

    List<CapaNatureAction> findByCapaAnalysis_Id(Long capaAnalysisId);
}
