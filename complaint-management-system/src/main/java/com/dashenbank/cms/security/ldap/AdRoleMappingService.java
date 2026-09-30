package com.dashenbank.cms.security.ldap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * AD Role Mapping Service.
 * Automatic AD attribute-to-role mapping is completely disabled.
 * Access authorization and role assignment are handled manually by
 * administrators
 * inside the Complaint Management System via the User Access Approval workflow.
 */
@Service
public class AdRoleMappingService {

    private static final Logger log = LoggerFactory.getLogger(AdRoleMappingService.class);

    public RoleResolution resolve(AdUserProfile profile) {
        log.info("AD automatic role mapping is disabled. User {} will require administrator approval.",
                profile != null ? profile.samAccountName() : "unknown");
        return RoleResolution.none();
    }

    public RoleResolution resolveFromTitle(String title) {
        return RoleResolution.none();
    }

    public RoleResolution resolveFromGroups(List<String> memberOf) {
        return RoleResolution.none();
    }

    public Map<String, String> publishedTitleMappings() {
        return Collections.emptyMap();
    }

    public Map<String, String> publishedGroupMappings() {
        return Collections.emptyMap();
    }
}
