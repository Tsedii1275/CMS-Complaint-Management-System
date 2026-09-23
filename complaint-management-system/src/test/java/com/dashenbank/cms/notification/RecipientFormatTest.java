package com.dashenbank.cms.notification;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipientFormatTest {

    @Test
    void normalizesEthiopianNumbersToE164() {
        assertEquals(Optional.of("+251912345678"), RecipientFormat.normalizePhone("0912345678", "251"));
        assertEquals(Optional.of("+251912345678"), RecipientFormat.normalizePhone("0912 345 678", "251"));
        assertEquals(Optional.of("+251912345678"), RecipientFormat.normalizePhone("912345678", "251"));
        assertEquals(Optional.of("+251912345678"), RecipientFormat.normalizePhone("251912345678", "251"));
        assertEquals(Optional.of("+251912345678"), RecipientFormat.normalizePhone("+251 91 234 5678", "251"));
        assertEquals(Optional.of("+251712345678"), RecipientFormat.normalizePhone("0712-345-678", "251"));
        assertEquals(Optional.of("+251912345678"), RecipientFormat.normalizePhone("00251912345678", "251"));
        assertEquals(Optional.of("+251912345678"), RecipientFormat.normalizePhone("+2510912345678", "251"));
        assertEquals(Optional.of("+251912345678"), RecipientFormat.normalizePhone("2510912345678", "251"));
    }

    @Test
    void rejectsInvalidPhoneNumbers() {
        assertTrue(RecipientFormat.normalizePhone("abc", "251").isEmpty());
        assertTrue(RecipientFormat.normalizePhone("12", "251").isEmpty());
        assertTrue(RecipientFormat.normalizePhone("+0123456789", "251").isEmpty());
        assertTrue(RecipientFormat.normalizePhone("0912345678", "").isEmpty());
        assertTrue(RecipientFormat.normalizePhone(null, "251").isEmpty());
    }

    @Test
    void validatesEmail() {
        assertEquals(Optional.of("abebe@dashenbanksc.com"), RecipientFormat.normalizeEmail(" abebe@dashenbanksc.com "));
        assertTrue(RecipientFormat.normalizeEmail("not-an-email").isEmpty());
        assertTrue(RecipientFormat.normalizeEmail("a@b").isEmpty());
    }

    @Test
    void masksRecipientsForLogs() {
        assertEquals("a***@dashenbanksc.com", RecipientFormat.maskEmail("abebe@dashenbanksc.com"));
        assertEquals("+251******678", RecipientFormat.maskPhone("+251912345678"));
        assertEquals("***", RecipientFormat.maskEmail("broken"));
    }
}
