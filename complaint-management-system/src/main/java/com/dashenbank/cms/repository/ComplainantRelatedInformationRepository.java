package com.dashenbank.cms.repository;

import com.dashenbank.cms.model.ComplainantRelatedInformation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ComplainantRelatedInformationRepository extends JpaRepository<ComplainantRelatedInformation, Long>,
        JpaSpecificationExecutor<ComplainantRelatedInformation> {
    Optional<ComplainantRelatedInformation> findByUniqueIdNo(String uniqueIdNo);

    boolean existsByUniqueIdNo(String uniqueIdNo);
}
