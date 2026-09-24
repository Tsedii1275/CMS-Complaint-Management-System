package com.dashenbank.cms.security.ldap;

import com.dashenbank.cms.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdRoleMappingServiceTest {

    private LdapProperties properties;
    private AdRoleMappingService service;

    @BeforeEach
    void setUp() {
        properties = new LdapProperties();
        service = new AdRoleMappingService(properties);
    }

    @Test
    void titleWinsOverGroups() {
        AdUserProfile profile = new AdUserProfile("abebe", "guid", "Abebe", "a@b.com",
                "Customer Care Officer", true,
                List.of("CN=CMS_CUSTOMER_CARE_TEAM_LEADER,OU=Groups,DC=dashenbank,DC=local"));
        RoleResolution resolution = service.resolve(profile);
        assertEquals(Role.ROLE_CUSTOMER_CARE_OFFICER, resolution.role());
        assertEquals("JOB_TITLE", resolution.source());
    }

    @Test
    void unknownTitleFallsBackToGroup() {
        AdUserProfile profile = new AdUserProfile("abebe", "guid", "Abebe", "a@b.com",
                "Something Unknown", true,
                List.of("CN=CMS_BRANCH_MANAGER,OU=Groups,DC=dashenbank,DC=local"));
        RoleResolution resolution = service.resolve(profile);
        assertEquals(Role.ROLE_BRANCH_MANAGER, resolution.role());
        assertEquals("AD_GROUP", resolution.source());
    }

    @Test
    void emptyTitleFallsBackToGroup() {
        AdUserProfile profile = new AdUserProfile("abebe", "guid", "Abebe", "a@b.com",
                "  ", true,
                List.of("CN=CMS_CUSTOMER_CARE_SENIOR_MANAGER,DC=dashenbank,DC=local"));
        RoleResolution resolution = service.resolve(profile);
        assertEquals(Role.ROLE_CUSTOMER_CARE_SENIOR_MANAGER, resolution.role());
    }

    @Test
    void unknownTitleAndNoGroupRejects() {
        AdUserProfile profile = new AdUserProfile("abebe", "guid", "Abebe", "a@b.com",
                "Unknown Title", true, List.of("CN=UNRELATED,DC=dashenbank,DC=local"));
        assertFalse(service.resolve(profile).resolved());
    }

    @Test
    void titleMatchingIsCaseAndWhitespaceInsensitive() {
        RoleResolution resolution = service.resolveFromTitle("  branch   manager ");
        assertEquals(Role.ROLE_BRANCH_MANAGER, resolution.role());
    }

    @Test
    void groupPriorityCanBeSelectedWithoutChangingCallers() {
        properties.setRolePriority(LdapRolePriority.GROUP_THEN_TITLE);
        AdUserProfile profile = new AdUserProfile("abebe", "guid", "Abebe", "a@b.com",
                "Customer Care Officer", true,
                List.of("CN=CMS_CUSTOMER_CARE_TEAM_LEADER,DC=dashenbank,DC=local"));
        RoleResolution resolution = service.resolve(profile);
        assertEquals(Role.ROLE_CUSTOMER_CARE_TEAM_LEADER, resolution.role());
        assertEquals("AD_GROUP", resolution.source());
    }

    @Test
    void configuredTitleOverlayWins() {
        properties.setTitleMappings("Chief Teller=ROLE_CONTACT_CENTER_AGENT");
        assertEquals(Role.ROLE_CONTACT_CENTER_AGENT, service.resolveFromTitle("Chief Teller").role());
    }

    @Test
    void everyDefaultTitleMapsToExistingCmsRole() {
        assertTrue(service.resolveFromTitle("Service Quality Director").resolved());
        assertTrue(service.resolveFromTitle("Department / Work Unit Manager").resolved());
        assertTrue(service.resolveFromTitle("Audit / Investigation Officer").resolved());
        assertEquals(Role.ROLE_CUSTOMER_CARE_TEAM_LEADER,
                service.resolveFromTitle("Customer Care Team Leader").role());
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
