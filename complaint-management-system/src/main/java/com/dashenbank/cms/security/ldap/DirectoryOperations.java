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

    /**
     * RFC 2696 paged search of enabled user objects. Used for Work Unit assignment.
     */
    List<AdUserProfile> searchDirectoryUsersPaged(int pageSize, int maxTotal);

    /**
     * Enabled people matching a typed name, username, email, or job title.
     * Default loads the assignment directory so tests can filter in memory.
     */
    default List<AdUserProfile> searchPeople(String query) {
        return searchDirectoryUsersPaged(500, 50_000);
    }

    /**
     * Organizational unit names under the Dashen Bank search base. Default empty
     * so test doubles need not implement LDAP OU search.
     */
    default List<String> searchOrganizationalUnitNames(int maxResults) {
        return List.of();
    }
}
