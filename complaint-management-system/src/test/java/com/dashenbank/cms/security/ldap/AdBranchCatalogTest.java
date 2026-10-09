package com.dashenbank.cms.security.ldap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdBranchCatalogTest {

    @Test
    void conventionalAndIfbWindowsAreBranches() {
        assertTrue(AdBranchCatalog.looksLikeBranchUnit("Bole Branch"));
        assertTrue(AdBranchCatalog.looksLikeBranchUnit("Bole IFB"));
        assertTrue(AdBranchCatalog.looksLikeBranchUnit("Megenagna Interest Free Banking"));
        assertTrue(AdBranchCatalog.looksLikeBranchUnit("IFB Window – Piassa"));
        assertTrue(AdBranchCatalog.isIfbBranchWindow("Bole IFB"));
    }

    @Test
    void headOfficeIfbDepartmentIsNotABranch() {
        assertFalse(AdBranchCatalog.looksLikeBranchUnit("Interest Free Banking Department"));
        assertTrue(AdBranchCatalog.isHeadOfficeIfbDepartment("Interest Free Banking Department"));
        assertFalse(AdBranchCatalog.looksLikeBranchUnit("Addis Ababa District"));
        assertFalse(AdBranchCatalog.looksLikeBranchUnit("Dashen Bank"));
    }

    @Test
    void districtChildOuWithoutTheWordBranchIsStillABranch() {
        String dn = "OU=Bole,OU=Addis Ababa District,OU=Dashen Bank,DC=dashenbank,DC=local";
        assertTrue(AdBranchCatalog.isDistrictChildBranch("Bole", dn));
        assertTrue(AdBranchCatalog.isAssignableBranch("Bole", dn));
        assertFalse(AdBranchCatalog.isDistrictChildBranch("Addis Ababa District", dn));
    }
}
