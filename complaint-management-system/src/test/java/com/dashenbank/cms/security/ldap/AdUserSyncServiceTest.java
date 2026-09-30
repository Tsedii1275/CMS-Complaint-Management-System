package com.dashenbank.cms.security.ldap;

import com.dashenbank.cms.model.AuthSource;
import com.dashenbank.cms.model.Role;
import com.dashenbank.cms.model.User;
import com.dashenbank.cms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdUserSyncServiceTest {

    private UserRepository userRepository;
    private AdUserSyncService syncService;
    private final AtomicLong ids = new AtomicLong();

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode(any())).thenReturn("{bcrypt}unused");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId(ids.incrementAndGet());
            }
            return user;
        });
        syncService = new AdUserSyncService(userRepository, encoder);
    }

    @Test
    void firstLoginCreatesPendingUser() {
        when(userRepository.findByObjectGuid("guid-1")).thenReturn(Optional.empty());
        when(userRepository.findByUsernameIgnoreCase("abebe")).thenReturn(Optional.empty());
        User created = syncService.upsertForLogin(officer("abebe", "guid-1"));
        assertEquals("abebe", created.getUsername());
        assertEquals(Role.ROLE_PENDING, created.getRole());
        assertFalse(created.isApproved());
        assertEquals("PENDING_APPROVAL", created.getApprovalStatus());
        assertEquals(AuthSource.AD, created.getAuthSource());
    }

    @Test
    void secondLoginUpdatesProfileWithoutOverwritingRole() {
        User existing = User.builder()
                .id(9L)
                .username("abebe")
                .email("old@dashenbank.com")
                .password("hash")
                .role(Role.ROLE_CUSTOMER_CARE_OFFICER)
                .authSource(AuthSource.AD)
                .approved(true)
                .approvalStatus("APPROVED")
                .objectGuid("guid-1")
                .enabled(true)
                .build();
        when(userRepository.findByObjectGuid("guid-1")).thenReturn(Optional.of(existing));
        User updated = syncService.upsertForLogin(officer("abebe", "guid-1"));
        assertEquals(9L, updated.getId());
        assertEquals("Abebe Bekele", updated.getFullName());
        assertEquals(Role.ROLE_CUSTOMER_CARE_OFFICER, updated.getRole());
        assertTrue(updated.isApproved());
    }

    @Test
    void randomPasswordIsNotTheAdPassword() {
        when(userRepository.findByObjectGuid(any())).thenReturn(Optional.empty());
        when(userRepository.findByUsernameIgnoreCase(any())).thenReturn(Optional.empty());
        User created = syncService.upsertForLogin(officer("abebe", "guid-1"));
        assertNotEquals("secret", created.getPassword());
    }

    private static AdUserProfile officer(String sam, String guid) {
        return new AdUserProfile(sam, guid, "Abebe Bekele", "abebe@dashenbank.com",
                "Customer Care Officer", true, List.of());
    }
}
