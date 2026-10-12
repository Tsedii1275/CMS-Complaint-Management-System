package com.dashenbank.cms.service;

import com.dashenbank.cms.model.Role;
import com.dashenbank.cms.model.User;
import com.dashenbank.cms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataSeederTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private DataSeeder seeder;

    @BeforeEach
    void setUp() {
        seeder = new DataSeeder(userRepository, passwordEncoder);
        ReflectionTestUtils.setField(seeder, "configuredAdminPassword", "Admin@Dashen2026!");
        when(passwordEncoder.encode(any())).thenReturn("hashed-seed");
    }

    @Test
    void existingUsersStayAndMissingOperationalAdminIsSeeded() throws Exception {
        User officer = User.builder()
                .username("abel")
                .role(Role.ROLE_CUSTOMER_CARE_OFFICER)
                .password("officer-hash")
                .build();
        User admin = User.builder()
                .username("admin")
                .role(Role.ROLE_ADMIN)
                .password("existing-hash")
                .fullName("Operational Admin")
                .build();
        when(userRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findByUsernameIgnoreCase("opadmin")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        seeder.run();

        assertEquals(Role.ROLE_SYSTEM_ADMIN, admin.getRole());
        assertEquals("existing-hash", admin.getPassword());
        assertEquals("abel", officer.getUsername());
        assertEquals("officer-hash", officer.getPassword());

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(2)).save(saved.capture());
        User opadmin = saved.getAllValues().stream()
                .filter(user -> "opadmin".equals(user.getUsername()))
                .findFirst()
                .orElseThrow();
        assertEquals(Role.ROLE_ADMIN, opadmin.getRole());
        assertEquals("Operational Admin", opadmin.getFullName());
        assertEquals("hashed-seed", opadmin.getPassword());
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void freshDatabaseSeedsAdminAsSystemAdministrator() throws Exception {
        when(userRepository.findByUsernameIgnoreCase(any())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        seeder.run();

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(2)).save(saved.capture());
        User admin = saved.getAllValues().get(0);
        User opadmin = saved.getAllValues().get(1);
        assertEquals("admin", admin.getUsername());
        assertEquals(Role.ROLE_SYSTEM_ADMIN, admin.getRole());
        assertEquals("opadmin", opadmin.getUsername());
        assertEquals(Role.ROLE_ADMIN, opadmin.getRole());
        verify(userRepository, never()).delete(any(User.class));
    }
}
