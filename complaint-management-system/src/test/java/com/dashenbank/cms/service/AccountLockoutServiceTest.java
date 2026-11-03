package com.dashenbank.cms.service;

import com.dashenbank.cms.model.AuthSource;
import com.dashenbank.cms.model.User;
import com.dashenbank.cms.repository.UserRepository;
import com.dashenbank.cms.security.SecuritySettings;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class AccountLockoutServiceTest {

    private final SecuritySettings settings = new SecuritySettings();
    private final AccountLockoutService service = new AccountLockoutService(
            mock(UserRepository.class), mock(SecurityAuditService.class), settings);

    private static User user(AuthSource source) {
        User user = new User();
        user.setUsername("person");
        user.setAuthSource(source);
        return user;
    }

    @Test
    void sixWrongPasswordsLockLocalAndDirectoryAccountsAlike() {
        for (AuthSource source : new AuthSource[] { AuthSource.LOCAL, AuthSource.AD }) {
            User user = user(source);
            for (int i = 1; i < settings.getLockoutThreshold(); i++) {
                service.recordFailedAttempt(user, "10.0.0.1");
                assertFalse(service.isLocked(user), source + " locked too early after " + i);
            }
            service.recordFailedAttempt(user, "10.0.0.1");
            assertTrue(service.isLocked(user), source + " not locked after the sixth wrong password");
        }
    }

    @Test
    void lockEndsAfterTheConfiguredDuration() {
        User user = user(AuthSource.AD);
        user.setAccountLocked(true);
        user.setLockoutTime(LocalDateTime.now().minusMinutes(settings.getLockoutDurationMinutes() + 1L));

        service.unlockIfElapsed(user, "10.0.0.1");

        assertFalse(service.isLocked(user));
        assertFalse(user.isAccountLocked());
    }
}
