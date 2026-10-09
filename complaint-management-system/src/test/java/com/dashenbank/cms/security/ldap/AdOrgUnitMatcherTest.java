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
}
