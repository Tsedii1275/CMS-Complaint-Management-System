package com.dashenbank.cms.repository;

import com.dashenbank.cms.model.FirstContactResolution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FirstContactResolutionRepository extends JpaRepository<FirstContactResolution, Long> {
    List<FirstContactResolution> findByComplaintId(String complaintId);
    Optional<FirstContactResolution> findTopByProcessInstanceIdOrderByIdDesc(String processInstanceId);
    Optional<FirstContactResolution> findTopByComplaintIdOrderByIdDesc(String complaintId);
}
