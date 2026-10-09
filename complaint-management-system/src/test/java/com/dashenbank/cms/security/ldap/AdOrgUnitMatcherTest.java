package com.dashenbank.cms.security.ldap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdOrgUnitMatcherTest {

    @Test
    void conventionalBranchNamesMatchWithOrWithoutBranchSuffix() {
        assertTrue(AdOrgUnitMatcher.sameUnit("Bole Branch", "Bole"));
        assertTrue(AdOrgUnitMatcher.sameUnit("Bole Branch", "Dashen Bank Bole Branch"));
        assertFalse(AdOrgUnitMatcher.sameUnit("Bole Branch", "Bole IFB"));
        assertTrue(AdOrgUnitMatcher.sameUnit("Bole IFB", "Bole Interest Free Banking"));
        assertTrue(AdOrgUnitMatcher.sameUnit("Customer Experience Department", "Customer Experience"));
    }
}
