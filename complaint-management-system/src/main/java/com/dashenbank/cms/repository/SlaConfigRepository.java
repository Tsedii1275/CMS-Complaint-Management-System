package com.dashenbank.cms.repository;

import com.dashenbank.cms.model.SlaConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SlaConfigRepository extends JpaRepository<SlaConfig, Long> {
    Optional<SlaConfig> findByConfigKey(String configKey);
    List<SlaConfig> findByConfigGroup(String configGroup);
    Optional<SlaConfig> findByConfigGroupAndPriority(String configGroup, String priority);
}
