package com.dashenbank.cms.service;

import com.dashenbank.cms.model.SecurityAuditEvent;
import com.dashenbank.cms.model.User;
import com.dashenbank.cms.repository.UserRepository;
import com.dashenbank.cms.security.SecurityPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AccountLockoutService {

    private final UserRepository userRepository;
    private final SecurityAuditService securityAuditService;

    public AccountLockoutService(UserRepository userRepository, SecurityAuditService securityAuditService) {
        this.userRepository = userRepository;
        this.securityAuditService = securityAuditService;
    }

    @Transactional
    public void unlockIfElapsed(User user, String ipAddress) {
        if (user == null || !user.isAccountLocked()) {
            return;
        }
        if (stillLocked(user)) {
            return;
        }
        user.setAccountLocked(false);
        user.setLockoutTime(null);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);
        securityAuditService.log(user.getUsername(), SecurityAuditEvent.ACCOUNT_UNLOCKED, ipAddress);
    }

    public boolean isLocked(User user) {
        return user != null && user.isAccountLocked() && stillLocked(user);
    }

    @Transactional
    public void recordFailedAttempt(User user, String ipAddress) {
        if (user == null) {
            return;
        }
        securityAuditService.log(user.getUsername(), SecurityAuditEvent.FAILED_LOGIN, ipAddress);
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= SecurityPolicy.LOCKOUT_THRESHOLD) {
            user.setAccountLocked(true);
            user.setLockoutTime(LocalDateTime.now());
            securityAuditService.log(user.getUsername(), SecurityAuditEvent.ACCOUNT_LOCKED, ipAddress);
        }
        userRepository.save(user);
    }

    @Transactional
    public void recordSuccessfulLogin(User user) {
        if (user == null) {
            return;
        }
        if (user.getFailedLoginAttempts() == 0 && !user.isAccountLocked()) {
            return;
        }
        user.setFailedLoginAttempts(0);
        user.setAccountLocked(false);
        user.setLockoutTime(null);
        userRepository.save(user);
    }

    private static boolean stillLocked(User user) {
        if (!user.isAccountLocked()) {
            return false;
        }
        LocalDateTime lockedAt = user.getLockoutTime();
        if (lockedAt == null) {
            return true;
        }
        return LocalDateTime.now().isBefore(lockedAt.plusMinutes(SecurityPolicy.LOCKOUT_DURATION_MINUTES));
    }
}
