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
    void dashenPrefixedBranchOuStillListsBranchManagerAndCsm() {
        AdOrganizationService service = service();
        AdUserProfile bm = new AdUserProfile("bm1", "1", "Sara", "s@d.com", "Branch Manager", true, List.of(),
                "Credit Department", null, "Bole");
        AdUserProfile csm = new AdUserProfile("csm1", "2", "Lidya", "l@d.com", "CSM", true, List.of(),
                null, "CN=Lidya,OU=Users,OU=Dashen Bole Branch,OU=Addis Ababa District,DC=dashenbank,DC=local",
                null);
        AdUserProfile teller = new AdUserProfile("t1", "3", "Teller", "t@d.com", "Teller", true, List.of(),
                null, null, "Bole Branch");
        String unitId = AdOrganizationService.idFor("Dashen Bole Branch");
        List<AdOrgOfficer> officers = service.officersFrom(List.of(bm, csm, teller), AdAssignmentScope.BRANCH, unitId);
        assertEquals(2, officers.size());
        assertTrue(officers.stream().anyMatch(o -> "Branch Manager".equals(o.title())));
        assertTrue(officers.stream().anyMatch(o -> "CSM".equals(o.title())));
        assertFalse(officers.stream().anyMatch(o -> "Teller".equals(o.title())));
    }

    @Test
    void dashenPrefixedBranchUsesOfficeLocationNotTheSharedOu() {
        AdOrganizationService service = service();
        String boleDn = "CN=Person,OU=Users,OU=Dashen Bole Branch,OU=Dashen Bank,DC=dashenbank,DC=local";
        AdUserProfile reliefCsm = new AdUserProfile("Sentayehua", "1", "Sentayehu", "s@d.com", "Relief CSM", true,
                List.of(), "Bole Branch", boleDn, "Bole Branch-012");
        AdUserProfile bbMgr = new AdUserProfile("GirmaAD", "2", "Girma", "g@d.com", "BBMgr", true, List.of(),
                "Bole Branch", boleDn, "Bole Branch-012");
        AdUserProfile ifbRm = new AdUserProfile("Hawah", "3", "Hawa", "h@d.com",
                "Branch Business Relationship Manager - IFB", true, List.of(), "Bole Branch", boleDn, "Bole Branch-012");
        AdUserProfile hidaseCsm = new AdUserProfile("Mikerm", "4", "Mikir", "m@d.com", "CSM", true, List.of(),
                "Hidase Sefer Branch", boleDn, "Hidase Sefer Branch-616");
        AdUserProfile kotebeRm = new AdUserProfile("TesfayeKe", "5", "Tesfaye", "t@d.com",
                "Branch Business Relationship Manager I", true, List.of(), "Kotebe Branch", boleDn, "Kotebe Branch-107");
        AdUserProfile cso = new AdUserProfile("Genetad", "6", "Genet", "g2@d.com", "CSO", true, List.of(),
                "Bole Branch", boleDn, "Bole Branch-012");
        String unitId = AdOrganizationService.idFor("Dashen Bole Branch");
        List<AdOrgOfficer> officers = service.officersFrom(
                List.of(reliefCsm, bbMgr, ifbRm, hidaseCsm, kotebeRm, cso), AdAssignmentScope.BRANCH, unitId);
        assertEquals(3, officers.size());
        assertTrue(officers.stream().anyMatch(o -> "Relief CSM".equals(o.title())));
        assertTrue(officers.stream().anyMatch(o -> "BBMgr".equals(o.title())));
        assertTrue(officers.stream().anyMatch(o -> "Branch Business Relationship Manager - IFB".equals(o.title())));
        assertFalse(officers.stream().anyMatch(o -> "Mikerm".equals(o.username())));
        assertFalse(officers.stream().anyMatch(o -> "TesfayeKe".equals(o.username())));
        assertFalse(officers.stream().anyMatch(o -> "CSO".equals(o.title())));
    }

    @Test
    void branchListIncludesDistrictChildOuEvenWithoutTheWordBranch() {
        AdUserProfile teller = new AdUserProfile("t1", "g", "Teller", "t@d.com", "Teller", true, List.of(),
                null, "CN=Teller,OU=Bole,OU=Addis Ababa District,OU=Dashen Bank,DC=dashenbank,DC=local", null);
        AdOrganizationService service = service();
        List<AdOrgUnit> branches = service.unitsFrom(List.of(teller), AdAssignmentScope.BRANCH);
        assertEquals(1, branches.size());
        assertEquals("Bole", branches.get(0).name());
        assertTrue(service.officersFrom(List.of(teller), AdAssignmentScope.BRANCH, branches.get(0).id()).isEmpty());
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
    void branchOfficersIncludeCsmAndActingTitlesEvenWhenOfficeOmitsBranchSuffix() {
        AdOrganizationService service = service();
        AdUserProfile bm = new AdUserProfile("bm1", "1", "Sara", "s@d.com", "Acting Branch Manager", true, List.of(),
                null, null, "Bole");
        AdUserProfile csm = new AdUserProfile("csm1", "2", "Lidya", "l@d.com", "Customer Service Manager", true,
                List.of(), null, null, "Bole Branch");
        AdUserProfile seniorCsm = new AdUserProfile("csm2", "3", "Hana", "h@d.com", "Senior Customer Service Manager",
                true, List.of(), null, null, "Bole Branch");
        AdUserProfile serviceMgr = new AdUserProfile("csm3", "4", "Miki", "m@d.com", "Service Manager", true, List.of(),
                null, null, "Bole Branch");
        AdUserProfile teller = new AdUserProfile("t1", "5", "Teller", "t@d.com", "Teller", true, List.of(),
                null, null, "Bole Branch");
        List<AdUserProfile> people = List.of(bm, csm, seniorCsm, serviceMgr, teller);
        String boleId = AdOrganizationService.idFor("Bole Branch");
        List<AdOrgOfficer> officers = service.officersFrom(people, AdAssignmentScope.BRANCH, boleId);
        assertEquals(4, officers.size());
        assertTrue(officers.stream().anyMatch(o -> "Acting Branch Manager".equals(o.title())));
        assertTrue(officers.stream().anyMatch(o -> "Customer Service Manager".equals(o.title())));
        assertTrue(officers.stream().anyMatch(o -> "Senior Customer Service Manager".equals(o.title())));
        assertTrue(officers.stream().anyMatch(o -> "Service Manager".equals(o.title())));
        assertFalse(officers.stream().anyMatch(o -> "Teller".equals(o.title())));
    }

    @Test
    void headOfficeLeadersIncludeDirectorAndManagerVariantsForTheSameDepartment() {
        AdOrganizationService service = service();
        AdUserProfile director = new AdUserProfile("d1", "1", "Daniel", "d@d.com", "Department Director", true,
                List.of(), "Customer Experience", null, "Head Office");
        AdUserProfile senior = new AdUserProfile("s1", "2", "Hanna", "h@d.com", "Acting Senior Manager", true, List.of(),
                "Customer Experience Department", null, null);
        AdUserProfile manager = new AdUserProfile("m1", "3", "Kedir", "k@d.com", "Head of Department", true, List.of(),
                "Customer Experience Department", null, null);
        AdUserProfile specialist = new AdUserProfile("x1", "4", "Officer", "o@d.com", "Analyst", true, List.of(),
                "Customer Experience Department", null, null);
        List<AdUserProfile> people = List.of(director, senior, manager, specialist);
        String deptId = AdOrganizationService.idFor("Customer Experience Department");
        List<AdOrgOfficer> leaders = service.officersFrom(people, AdAssignmentScope.HEAD_OFFICE_DEPARTMENT, deptId);
        assertEquals(3, leaders.size());
        assertTrue(leaders.stream().anyMatch(o -> "Department Director".equals(o.title())));
        assertTrue(leaders.stream().anyMatch(o -> "Acting Senior Manager".equals(o.title())));
        assertTrue(leaders.stream().anyMatch(o -> "Head of Department".equals(o.title())));
        assertFalse(leaders.stream().anyMatch(o -> "Analyst".equals(o.title())));
    }

    @Test
    void configuredTitleMappingsAddBankSpecificTitlesWithoutReplacingTheCatalog() {
        LdapProperties props = new LdapProperties();
        props.setTitleMappings("Cluster Champion=ROLE_CUSTOMER_SERVICE_MANAGER;Value Stream Owner=ROLE_SENIOR_MANAGER");
        AdOrganizationService service = new AdOrganizationService(new DirectoryOperations() {
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
                return List.of();
            }

            @Override
            public List<AdUserProfile> searchDirectoryUsersPaged(int pageSize, int maxTotal) {
                return List.of();
            }
        }, props);
        AdUserProfile cluster = new AdUserProfile("csmx", "g", "Selam", "s@d.com", "Cluster Champion", true,
                List.of(), null, null, "Hawassa Branch");
        AdUserProfile product = new AdUserProfile("dirx", "g", "Yonas", "y@d.com", "Value Stream Owner", true, List.of(),
                "Digital Banking Department", null, null);
        assertTrue(service.matchesConfiguredScope(cluster, AdAssignmentScope.BRANCH));
        assertTrue(service.matchesConfiguredScope(product, AdAssignmentScope.HEAD_OFFICE_DEPARTMENT));
        assertTrue(AdOrganizationService.matchesScope(
                new AdUserProfile("bm1", "g", "Sara", "s@d.com", "Branch Manager", true, List.of(), null, null,
                        "Hawassa Branch"),
                AdAssignmentScope.BRANCH));
    }

    @Test
    void ifbOfficeWithoutTheWordBranchIsAValidAssignmentDestination() {
        AdUserProfile ifbManager = new AdUserProfile("ifb1", "g", "Selam", "s@d.com", "IFB Manager", true, List.of(),
                null, "CN=Selam,OU=Bole IFB,OU=Dashen Bank,DC=dashenbank,DC=local", "Bole IFB");
        assertEquals("Bole IFB", AdOrganizationService.orgUnitName(ifbManager, AdAssignmentScope.BRANCH));
        assertTrue(AdOrganizationService.matchesScope(ifbManager, AdAssignmentScope.BRANCH));
        assertFalse(AdOrganizationService.matchesScope(ifbManager, AdAssignmentScope.HEAD_OFFICE_DEPARTMENT));
        AdOrganizationService service = service();
        List<AdOrgUnit> branches = service.unitsFrom(List.of(ifbManager), AdAssignmentScope.BRANCH);
        assertEquals(1, branches.size());
        assertEquals("Bole IFB", branches.get(0).name());
    }

    @Test
    void headOfficeIfbDepartmentIsNotListedAsABranch() {
        AdUserProfile hoIfb = new AdUserProfile("ifbho", "g", "Kedir", "k@d.com", "Director", true, List.of(),
                "Interest Free Banking Department", null, "Head Office");
        assertFalse(AdOrganizationService.matchesScope(hoIfb, AdAssignmentScope.BRANCH));
        assertTrue(AdOrganizationService.matchesScope(hoIfb, AdAssignmentScope.HEAD_OFFICE_DEPARTMENT));
    }

    @Test
    void branchMasterMergesAdOusIncludingIfbWithPeopleDerivedNames() {
        List<AdOrgUnit> merged = AdOrganizationService.mergeBranchMaster(
                List.of("Bole Branch", "Bole IFB", "Interest Free Banking Department", "Addis Ababa District"),
                List.of(new AdOrgUnit("x", "Piassa Branch")));
        assertTrue(merged.stream().anyMatch(u -> "Bole Branch".equals(u.name())));
        assertTrue(merged.stream().anyMatch(u -> "Bole IFB".equals(u.name())));
        assertTrue(merged.stream().anyMatch(u -> "Piassa Branch".equals(u.name())));
        assertFalse(merged.stream().anyMatch(u -> "Interest Free Banking Department".equals(u.name())));
        assertFalse(merged.stream().anyMatch(u -> "Addis Ababa District".equals(u.name())));
    }

    @Test
    void branchesEndpointUsesOuMasterWhenPeopleAreMissing() {
        AdOrganizationService service = new AdOrganizationService(new DirectoryOperations() {
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
                return List.of();
            }

            @Override
            public List<AdUserProfile> searchDirectoryUsersPaged(int pageSize, int maxTotal) {
                return List.of();
            }

            @Override
            public List<String> searchOrganizationalUnitNames(int maxResults) {
                return List.of("Hawassa Branch", "Adama IFB");
            }
        }, new LdapProperties());
        List<AdOrgUnit> branches = service.branches();
        assertEquals(2, branches.size());
        assertTrue(branches.stream().anyMatch(u -> "Adama IFB".equals(u.name())));
        assertTrue(branches.stream().anyMatch(u -> "Hawassa Branch".equals(u.name())));
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
