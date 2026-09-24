package com.dashenbank.cms.security.ldap;

import com.dashenbank.cms.model.User;
import org.springframework.stereotype.Service;

@Service
public class ActiveDirectoryAuthService {

    private final DirectoryOperations directory;
    private final AdUserSyncService syncService;

    public ActiveDirectoryAuthService(DirectoryOperations directory, AdUserSyncService syncService) {
        this.directory = directory;
        this.syncService = syncService;
    }

    public User login(String username, String password) {
        AdUserProfile profile = directory.authenticate(username, password);
        return syncService.upsertForLogin(profile);
    }
}
