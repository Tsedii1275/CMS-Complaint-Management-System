package com.dashenbank.cms.security.ldap;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdWorkUnitTitleMatcherTest {

    @Test
    void normalizesDashAndRomanSuffixes() {
        assertEquals("director service quality", AdWorkUnitTitleMatcher.normalize("Director – Service Quality"));
        assertEquals("branch manager ii", AdWorkUnitTitleMatcher.normalize("Branch Manager II"));
    }

    @Test
    void mapsBranchManagerVariants() {
        assertTrue(AdWorkUnitTitleMatcher.matchesTitle("Branch Manager", AdAssignmentScope.BRANCH));
        assertTrue(AdWorkUnitTitleMatcher.matchesTitle("Branch Manager I", AdAssignmentScope.BRANCH));
        assertTrue(AdWorkUnitTitleMatcher.matchesTitle("Branch Manager II", AdAssignmentScope.BRANCH));
        assertTrue(AdWorkUnitTitleMatcher.matchesTitle("Senior Branch Manager", AdAssignmentScope.BRANCH));
        assertFalse(AdWorkUnitTitleMatcher.matchesTitle("Senior Branch Manager",
                AdAssignmentScope.HEAD_OFFICE_DEPARTMENT));
    }

    @Test
    void mapsDistrictAndHeadOfficeDirectors() {
        assertTrue(AdWorkUnitTitleMatcher.matchesTitle("District Director", AdAssignmentScope.DISTRICT));
        assertTrue(AdWorkUnitTitleMatcher.matchesTitle("District Business Director", AdAssignmentScope.DISTRICT));
        assertTrue(AdWorkUnitTitleMatcher.matchesTitle("Director – Service Quality",
                AdAssignmentScope.HEAD_OFFICE_DEPARTMENT));
        assertTrue(AdWorkUnitTitleMatcher.matchesTitle("Director – Customer Experience",
                AdAssignmentScope.HEAD_OFFICE_DEPARTMENT));
        assertTrue(AdWorkUnitTitleMatcher.matchesTitle("Senior Manager", AdAssignmentScope.HEAD_OFFICE_DEPARTMENT));
        assertFalse(AdWorkUnitTitleMatcher.matchesTitle("District Director", AdAssignmentScope.HEAD_OFFICE_DEPARTMENT));
        assertFalse(AdWorkUnitTitleMatcher.matchesTitle("Operational Manager",
                AdAssignmentScope.HEAD_OFFICE_DEPARTMENT));
    }

    @Test
    void groupFallbackUsesGroupCn() {
        assertTrue(AdWorkUnitTitleMatcher.matchesGroup(
                List.of("CN=Operational Manager,OU=Groups,DC=dashenbank,DC=local"), AdAssignmentScope.DISTRICT));
    }
}
