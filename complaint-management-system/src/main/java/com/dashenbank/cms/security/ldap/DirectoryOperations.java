package com.dashenbank.cms.security.ldap;

import java.util.List;
import java.util.Optional;

public interface DirectoryOperations {

    boolean configured();

    DirectoryHealth health();

    /**
     * Bind as the user to validate the password, then return the AD profile.
     */
    AdUserProfile authenticate(String username, String password);

    Optional<AdUserProfile> findBySamAccountName(String username);

    List<AdUserProfile> searchDirectoryUsers(int maxResults);
}
