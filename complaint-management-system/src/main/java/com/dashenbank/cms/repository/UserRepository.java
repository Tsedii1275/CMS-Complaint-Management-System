package com.dashenbank.cms.repository;

import com.dashenbank.cms.model.AuthSource;
import com.dashenbank.cms.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByUsernameIgnoreCase(String username);
    Optional<User> findByObjectGuid(String objectGuid);
    List<User> findByAuthSource(AuthSource authSource);
    long countByAuthSource(AuthSource authSource);
    Boolean existsByUsername(String username);
}
