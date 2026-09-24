package com.dashenbank.cms.repository;

import com.dashenbank.cms.model.LdapSyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LdapSyncStatusRepository extends JpaRepository<LdapSyncStatus, Long> {
}
