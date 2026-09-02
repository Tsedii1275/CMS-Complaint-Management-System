package com.dashenbank.cms.repository;

import com.dashenbank.cms.model.PasswordHistory;
import com.dashenbank.cms.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, Long> {
    List<PasswordHistory> findByUserOrderByCreatedAtDesc(User user);
}
