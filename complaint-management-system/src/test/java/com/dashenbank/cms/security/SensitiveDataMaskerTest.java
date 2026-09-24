package com.dashenbank.cms.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SensitiveDataMaskerTest {

    @Test
    void masksPhoneKeepingPrefixAndLastTwoDigits() {
        assertEquals("0911****44", SensitiveDataMasker.phone("0911223344"));
    }

    @Test
    void masksEmailKeepingFirstFourLocalCharacters() {
        assertEquals("cust****@email.com", SensitiveDataMasker.email("customer@email.com"));
    }

    @Test
    void masksAccountNumberKeepingLastFourDigits() {
        assertEquals("****5678", SensitiveDataMasker.accountNumber("100012345678"));
    }
}
