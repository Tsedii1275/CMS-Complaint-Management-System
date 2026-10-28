package com.dashenbank.cms.repository;

import com.dashenbank.cms.model.ComplaintCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ComplaintCategoryRepository extends JpaRepository<ComplaintCategory, Long> {

    List<ComplaintCategory> findByActiveTrueOrderBySortOrderAscNameAsc();

    List<ComplaintCategory> findAllByOrderBySortOrderAscNameAsc();

    Optional<ComplaintCategory> findByNameKey(String nameKey);

    Optional<ComplaintCategory> findTopByOrderBySortOrderDesc();
}
