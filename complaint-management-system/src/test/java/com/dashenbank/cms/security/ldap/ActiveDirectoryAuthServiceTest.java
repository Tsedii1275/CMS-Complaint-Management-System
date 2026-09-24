package com.dashenbank.cms.security.ldap;

import com.dashenbank.cms.model.AuthSource;
import com.dashenbank.cms.model.Role;
import com.dashenbank.cms.model.User;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ActiveDirectoryAuthServiceTest {

    @Test
    void loginSyncsAfterSuccessfulBind() {
        DirectoryOperations directory = mock(DirectoryOperations.class);
        AdUserSyncService sync = mock(AdUserSyncService.class);
        AdUserProfile profile = new AdUserProfile("abebe", "g", "Abebe", "a@b.com",
                "Branch Manager", true, List.of());
        User user = User.builder().username("abebe").email("a@b.com").password("x")
                .role(Role.ROLE_BRANCH_MANAGER).authSource(AuthSource.AD).enabled(true).build();
        when(directory.authenticate("abebe", "pw")).thenReturn(profile);
        when(sync.upsertForLogin(profile)).thenReturn(user);
        User loggedIn = new ActiveDirectoryAuthService(directory, sync).login("abebe", "pw");
        assertEquals("abebe", loggedIn.getUsername());
        assertEquals(Role.ROLE_BRANCH_MANAGER, loggedIn.getRole());
    }

    @Test
    void invalidPasswordDoesNotProvision() {
        DirectoryOperations directory = mock(DirectoryOperations.class);
        when(directory.authenticate("abebe", "bad"))
                .thenThrow(new DirectoryAuthenticationException("Invalid username or password"));
        ActiveDirectoryAuthService service = new ActiveDirectoryAuthService(directory, mock(AdUserSyncService.class));
        assertThrows(DirectoryAuthenticationException.class, () -> service.login("abebe", "bad"));
    }

    @Test
    void directoryDownIsUnavailable() {
        DirectoryOperations directory = mock(DirectoryOperations.class);
        when(directory.authenticate("abebe", "pw"))
                .thenThrow(new DirectoryUnavailableException("timeout"));
        ActiveDirectoryAuthService service = new ActiveDirectoryAuthService(directory, mock(AdUserSyncService.class));
        assertThrows(DirectoryUnavailableException.class, () -> service.login("abebe", "pw"));
    }
}
