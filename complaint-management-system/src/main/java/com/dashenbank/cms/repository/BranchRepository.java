package com.dashenbank.cms.repository;

import com.dashenbank.cms.model.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {
    Optional<Branch> findByCode(String code);
    Optional<Branch> findByName(String name);
    List<Branch> findByDistrictId(Long districtId);
}
