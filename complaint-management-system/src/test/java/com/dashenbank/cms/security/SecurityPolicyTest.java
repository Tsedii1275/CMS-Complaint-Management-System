package com.dashenbank.cms.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityPolicyTest {

    @Test
    void acceptsComplexPasswordsOfAtLeastTwelveCharacters() {
        assertTrue(SecurityPolicy.meetsPasswordPolicy("ComplaintSys1"));
        assertTrue(SecurityPolicy.meetsPasswordPolicy("SecureBank123"));
        assertTrue(SecurityPolicy.meetsPasswordPolicy("Admin@Dashen2026!"));
    }

    @Test
    void rejectsShortOrIncompletePasswords() {
        assertFalse(SecurityPolicy.meetsPasswordPolicy("Dashen@2026"));
        assertFalse(SecurityPolicy.meetsPasswordPolicy("alllowercase1"));
        assertFalse(SecurityPolicy.meetsPasswordPolicy("ALLUPPERCASE1"));
        assertFalse(SecurityPolicy.meetsPasswordPolicy("NoDigitsHere!"));
        assertFalse(SecurityPolicy.meetsPasswordPolicy("Short1A"));
    }
}
