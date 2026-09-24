package com.dashenbank.cms.service;

import com.dashenbank.cms.model.PasswordHistory;
import com.dashenbank.cms.model.User;
import com.dashenbank.cms.repository.PasswordHistoryRepository;
import com.dashenbank.cms.security.SecurityPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
public class PasswordPolicyService {

    private static final ZoneId SYSTEM_ZONE = ZoneId.of("Africa/Addis_Ababa");

    public enum Validation {
        VALID,
        INVALID_POLICY,
        HISTORY_REUSE
    }

    private final PasswordEncoder passwordEncoder;
    private final PasswordHistoryRepository passwordHistoryRepository;

    public PasswordPolicyService(PasswordEncoder passwordEncoder,
            PasswordHistoryRepository passwordHistoryRepository) {
        this.passwordEncoder = passwordEncoder;
        this.passwordHistoryRepository = passwordHistoryRepository;
    }

    public Validation validateNewPassword(User user, String rawPassword) {
        if (!SecurityPolicy.meetsPasswordPolicy(rawPassword)) {
            return Validation.INVALID_POLICY;
        }
        if (isReused(user, rawPassword)) {
            return Validation.HISTORY_REUSE;
        }
        return Validation.VALID;
    }

    public boolean isExpired(User user) {
        if (user == null || user.getAuthSource() == com.dashenbank.cms.model.AuthSource.AD
                || user.getPasswordExpiryDate() == null) {
            return false;
        }
        return !user.getPasswordExpiryDate().isAfter(LocalDateTime.now(SYSTEM_ZONE));
    }

    public Map<String, Object> passwordStatus(User user) {
        boolean expired = isExpired(user);
        long daysRemaining = 0;
        if (user != null && user.getPasswordExpiryDate() != null && !expired) {
            daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(SYSTEM_ZONE),
                    user.getPasswordExpiryDate().toLocalDate());
        }
        boolean expiringSoon = !expired && daysRemaining >= 0 && daysRemaining <= SecurityPolicy.PASSWORD_WARNING_DAYS;
        return Map.of(
                "daysRemaining", daysRemaining,
                "expiringSoon", expiringSoon,
                "expired", expired);
    }

    @Transactional
    public void assignPassword(User user, String rawPassword) {
        String previousHash = user.getPassword();
        if (previousHash != null && !previousHash.isBlank() && user.getId() != null) {
            passwordHistoryRepository.save(PasswordHistory.builder()
                    .user(user)
                    .passwordHash(previousHash)
                    .build());
            passwordHistoryRepository.flush();
            pruneHistory(user);
        }
        user.setPassword(passwordEncoder.encode(rawPassword));
        LocalDateTime changedAt = LocalDateTime.now(SYSTEM_ZONE);
        user.setPasswordChangedAt(changedAt);
        user.setPasswordExpiryDate(changedAt.plusDays(SecurityPolicy.PASSWORD_EXPIRY_DAYS));
        user.setMustChangePassword(false);
        user.setFailedLoginAttempts(0);
        user.setAccountLocked(false);
        user.setLockoutTime(null);
    }

    public void stampInitialPasswordMetadata(User user) {
        LocalDateTime changedAt = LocalDateTime.now(SYSTEM_ZONE);
        user.setPasswordChangedAt(changedAt);
        user.setPasswordExpiryDate(changedAt.plusDays(SecurityPolicy.PASSWORD_EXPIRY_DAYS));
    }

    private boolean isReused(User user, String rawPassword) {
        if (user == null || rawPassword == null) {
            return false;
        }
        if (user.getPassword() != null && passwordEncoder.matches(rawPassword, user.getPassword())) {
            return true;
        }
        if (user.getId() == null) {
            return false;
        }
        List<PasswordHistory> history = passwordHistoryRepository.findByUserOrderByCreatedAtDesc(user);
        int limit = Math.min(SecurityPolicy.PASSWORD_HISTORY_COUNT, history.size());
        for (int i = 0; i < limit; i++) {
            if (passwordEncoder.matches(rawPassword, history.get(i).getPasswordHash())) {
                return true;
            }
        }
        return false;
    }

    private void pruneHistory(User user) {
        List<PasswordHistory> history = passwordHistoryRepository.findByUserOrderByCreatedAtDesc(user);
        if (history.size() <= SecurityPolicy.PASSWORD_HISTORY_COUNT) {
            return;
        }
        passwordHistoryRepository.deleteAll(history.subList(SecurityPolicy.PASSWORD_HISTORY_COUNT, history.size()));
    }
}
