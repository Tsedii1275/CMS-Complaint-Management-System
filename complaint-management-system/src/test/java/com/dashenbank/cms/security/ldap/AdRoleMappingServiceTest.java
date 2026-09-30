package com.dashenbank.cms.security.ldap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AdRoleMappingServiceTest {

    private AdRoleMappingService service;

    @BeforeEach
    void setUp() {
        service = new AdRoleMappingService();
    }

    @Test
    void resolveAlwaysReturnsNoneBecauseAutoMappingIsDisabled() {
        AdUserProfile profile = new AdUserProfile("abebe", "guid", "Abebe", "a@b.com",
                "Customer Care Officer", true,
                List.of("CN=CMS_CUSTOMER_CARE_TEAM_LEADER,OU=Groups,DC=dashenbank,DC=local"));
        RoleResolution resolution = service.resolve(profile);
        assertFalse(resolution.resolved());
    }

    @Test
    void resolveFromTitleAlwaysReturnsNone() {
        RoleResolution resolution = service.resolveFromTitle("Branch Manager");
        assertFalse(resolution.resolved());
    }

    @Test
    void resolveFromGroupsAlwaysReturnsNone() {
        RoleResolution resolution = service.resolveFromGroups(List.of("CN=CMS_ADMIN,DC=dashenbank,DC=local"));
        assertFalse(resolution.resolved());
    }

    @Test
    void windowsPathConvertsToBindDn() {
        String dn = LdapProperties.toLdapDn(
                "dashenbank.local/Dashen Bank/Dashen Head Office/Service User/CASHCOMP");
        assertEquals("CN=CASHCOMP,OU=Service User,OU=Dashen Head Office,OU=Dashen Bank,DC=dashenbank,DC=local", dn);
    }

    @Test
    void bindUsernameBecomesUpn() {
        LdapProperties props = new LdapProperties();
        props.setBindDn("CASHCOMP");
        props.setDomain("dashenbank.local");
        assertEquals("CASHCOMP@dashenbank.local", props.resolvedBindDn());
    }
}
