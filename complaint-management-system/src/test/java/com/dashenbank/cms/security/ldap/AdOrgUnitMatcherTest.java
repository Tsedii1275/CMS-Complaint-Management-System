package com.dashenbank.cms.security.ldap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdOrgUnitMatcherTest {

    @Test
    void conventionalBranchNamesMatchWithOrWithoutBranchSuffix() {
        assertTrue(AdOrgUnitMatcher.sameUnit("Bole Branch", "Bole"));
        assertTrue(AdOrgUnitMatcher.sameUnit("Bole Branch", "Dashen Bank Bole Branch"));
        assertTrue(AdOrgUnitMatcher.sameUnit("Bole", "Dashen Bole Branch"));
        assertTrue(AdOrgUnitMatcher.sameUnit("Mekelle", "Dashen Mekelle Branch"));
        assertFalse(AdOrgUnitMatcher.sameUnit("Bole Branch", "Bole IFB"));
        assertTrue(AdOrgUnitMatcher.sameUnit("Bole IFB", "Bole Interest Free Banking"));
        assertTrue(AdOrgUnitMatcher.sameUnit("Customer Experience Department", "Customer Experience"));
    }

    @Test
    void officeNamesWithBranchCodesMatchDashenPrefixedOus() {
        assertTrue(AdOrgUnitMatcher.sameUnit("Bole Branch-012", "Dashen Bole Branch"));
        assertTrue(AdOrgUnitMatcher.sameUnit("Bole Branch", "Dashen Bole Branch"));
        assertFalse(AdOrgUnitMatcher.sameUnit("Hidase Sefer Branch-616", "Dashen Bole Branch"));
        assertFalse(AdOrgUnitMatcher.sameUnit("Kotebe Branch-107", "Dashen Bole Branch"));
    }

    @Test
    void branchPlaceKeepsNumberedAndIfbNeighboursSeparate() {
        assertTrue(AdOrgUnitMatcher.sameBranchPlace("Kotebe Branch-107", "Dashen Kotebe Branch"));
        assertTrue(AdOrgUnitMatcher.sameBranchPlace("Ayer Tena Branch", "Dashen Ayer Tena Branch"));
        assertTrue(AdOrgUnitMatcher.sameBranchPlace("Bahir Dar Branch-020", "Dashen Bahirdar Branch"));
        assertTrue(AdOrgUnitMatcher.sameBranchPlace("IFB Ayer tena Ansuar Branch-F25",
                "Dashen IFB - Ayer tena Ansuar Branch"));
        assertTrue(AdOrgUnitMatcher.sameBranchPlace("FIFB NUR BRANCH-J04", "Dashen IFB - Nur Branch"));
        assertTrue(AdOrgUnitMatcher.sameBranchPlace("IFB-Mecca Branch-I49", "Dashen IFB - Mecca Branch"));
        assertFalse(AdOrgUnitMatcher.sameBranchPlace("Kotebe Messalemia Branch-713", "Dashen Kotebe Branch"));
        assertFalse(AdOrgUnitMatcher.sameBranchPlace("Ayer Tena 04 Branch-694", "Dashen Ayer Tena Branch"));
        assertFalse(AdOrgUnitMatcher.sameBranchPlace("Ayer Tena Branch-075", "Dashen Ayer Tena 04 Branch"));
        assertFalse(AdOrgUnitMatcher.sameBranchPlace("IFB Ayer tena Ansuar Branch-F25", "Dashen Ayer Tena Branch"));
        assertFalse(AdOrgUnitMatcher.sameBranchPlace("Corporate Banking Department", "Dashen Bole Branch"));
    }
}
