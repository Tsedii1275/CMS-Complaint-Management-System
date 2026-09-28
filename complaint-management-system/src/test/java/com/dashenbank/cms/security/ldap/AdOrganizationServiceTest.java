package com.dashenbank.cms.security.ldap;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdOrganizationServiceTest {

    @Test
    void districtsComeFromDistrictDirectorAndOperationalManager() {
        AdOrganizationService service = service();
        List<AdOrgUnit> districts = service.unitsFrom(sampleDirectory(), AdAssignmentScope.DISTRICT);
        assertEquals(1, districts.size());
        assertEquals("Addis Ababa District", districts.get(0).name());
        List<AdOrgOfficer> officers = service.officersFrom(sampleDirectory(), AdAssignmentScope.DISTRICT,
                districts.get(0).id());
        assertEquals(2, officers.size());
        assertEquals("db12345", officers.get(0).username());
        assertEquals("District Director", officers.get(0).title());
    }

    @Test
    void branchesReturnOnlyBranchManagers() {
        AdOrganizationService service = service();
        List<AdOrgUnit> branches = service.unitsFrom(sampleDirectory(), AdAssignmentScope.BRANCH);
        assertEquals("Bole Branch", branches.get(0).name());
        List<AdOrgOfficer> managers = service.officersFrom(sampleDirectory(), AdAssignmentScope.BRANCH,
                branches.get(0).id());
        assertEquals(1, managers.size());
        assertEquals("Branch Manager", managers.get(0).title());
    }

    @Test
    void headOfficeLeadersExcludeDistrictAndBranchTitles() {
        AdOrganizationService service = service();
        List<AdOrgUnit> depts = service.unitsFrom(sampleDirectory(), AdAssignmentScope.HEAD_OFFICE_DEPARTMENT);
        assertEquals("Customer Experience Department", depts.get(0).name());
        List<AdOrgOfficer> leaders = service.officersFrom(sampleDirectory(), AdAssignmentScope.HEAD_OFFICE_DEPARTMENT,
                depts.get(0).id());
        assertEquals(2, leaders.size());
        assertTrue(leaders.stream().anyMatch(o -> "Senior Manager".equals(o.title())));
        assertTrue(leaders.stream().anyMatch(o -> "Director".equals(o.title())));
        assertFalse(leaders.stream().anyMatch(o -> o.title().toLowerCase().contains("district")));
    }

    @Test
    void groupFallbackMatchesWhenTitleIsMissing() {
        AdUserProfile profile = new AdUserProfile("om1", "g", "Alemu", "a@b.com", "", true,
                List.of("CN=Operational Manager,OU=Groups,DC=dashenbank,DC=local"),
                "East District", null, null);
        assertTrue(AdOrganizationService.matchesScope(profile, AdAssignmentScope.DISTRICT));
    }

    @Test
    void orgUnitFallsBackToOuInDistinguishedName() {
        AdUserProfile profile = new AdUserProfile("bm1", "g", "Biniam", "b@b.com", "Branch Manager", true, List.of(),
                "Credit Department", "CN=Biniam,OU=Bole Branch,OU=Addis Ababa District,DC=dashenbank,DC=local", null);
        assertEquals("Bole Branch", AdOrganizationService.orgUnitName(profile, AdAssignmentScope.BRANCH));
    }

    @Test
    void branchPrefersOfficeOverDepartment() {
        AdUserProfile profile = new AdUserProfile("bm1", "g", "Biniam", "b@b.com", "Branch Manager II", true, List.of(),
                "Credit Department", null, "Bole Branch");
        assertEquals("Bole Branch", AdOrganizationService.orgUnitName(profile, AdAssignmentScope.BRANCH));
        assertTrue(AdOrganizationService.matchesScope(profile, AdAssignmentScope.BRANCH));
        assertFalse(AdOrganizationService.matchesScope(profile, AdAssignmentScope.HEAD_OFFICE_DEPARTMENT));
    }

    @Test
    void headOfficePrefersDepartmentOverOffice() {
        AdUserProfile profile = new AdUserProfile("sm1", "g", "Hanna", "h@d.com", "Director – Customer Experience",
                true, List.of(), "Customer Experience Department", null, "Head Office Campus");
        assertEquals("Customer Experience Department",
                AdOrganizationService.orgUnitName(profile, AdAssignmentScope.HEAD_OFFICE_DEPARTMENT));
        assertTrue(AdOrganizationService.matchesScope(profile, AdAssignmentScope.HEAD_OFFICE_DEPARTMENT));
    }

    @Test
    void districtBusinessDirectorMapsToDistrictNotHeadOffice() {
        AdUserProfile profile = new AdUserProfile("dd1", "g", "Mulu", "m@d.com", "District Business Director", true,
                List.of(), null, null, "East District");
        assertTrue(AdOrganizationService.matchesScope(profile, AdAssignmentScope.DISTRICT));
        assertFalse(AdOrganizationService.matchesScope(profile, AdAssignmentScope.HEAD_OFFICE_DEPARTMENT));
    }

    @Test
    void knownTitleDoesNotMoveScopeViaGroupFallback() {
        AdUserProfile profile = new AdUserProfile("bm1", "g", "Sara", "s@d.com", "Senior Branch Manager", true,
                List.of("CN=Director,OU=Groups,DC=dashenbank,DC=local"), "Bole Branch", null, "Bole Branch");
        assertTrue(AdOrganizationService.matchesScope(profile, AdAssignmentScope.BRANCH));
        assertFalse(AdOrganizationService.matchesScope(profile, AdAssignmentScope.HEAD_OFFICE_DEPARTMENT));
    }

    private static AdOrganizationService service() {
        return new AdOrganizationService(new DirectoryOperations() {
            @Override
            public boolean configured() {
                return true;
            }

            @Override
            public DirectoryHealth health() {
                return new DirectoryHealth(true, "ldaps://example", "ok");
            }

            @Override
            public AdUserProfile authenticate(String username, String password) {
                throw new UnsupportedOperationException();
            }

            @Override
            public java.util.Optional<AdUserProfile> findBySamAccountName(String username) {
                return java.util.Optional.empty();
            }

            @Override
            public List<AdUserProfile> searchDirectoryUsers(int maxResults) {
                return sampleDirectory();
            }

            @Override
            public List<AdUserProfile> searchDirectoryUsersPaged(int pageSize, int maxTotal) {
                return sampleDirectory();
            }
        }, new LdapProperties());
    }

    private static List<AdUserProfile> sampleDirectory() {
        return List.of(
                new AdUserProfile("db12345", "1", "Abebe Kebede", "a@d.com", "District Director", true, List.of(),
                        "Addis Ababa District", null, null),
                new AdUserProfile("db67890", "2", "Alemu Bekele", "b@d.com", "Operational Manager", true, List.of(),
                        "Addis Ababa District", null, null),
                new AdUserProfile("bm99", "3", "Sara Tadesse", "s@d.com", "Branch Manager", true, List.of(),
                        "Bole Branch", null, null),
                new AdUserProfile("sm1", "4", "Hanna Worku", "h@d.com", "Senior Manager", true, List.of(),
                        "Customer Experience Department", null, null),
                new AdUserProfile("dir1", "5", "Daniel Haile", "d@d.com", "Director", true, List.of(),
                        "Customer Experience Department", null, null),
                new AdUserProfile("cco", "6", "Officer", "o@d.com", "Customer Care Officer", true, List.of(),
                        "CMD", null, null));
    }
}
