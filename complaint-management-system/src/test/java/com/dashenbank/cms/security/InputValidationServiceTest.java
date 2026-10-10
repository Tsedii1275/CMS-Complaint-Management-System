package com.dashenbank.cms.security;

import com.dashenbank.cms.exception.InputValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputValidationServiceTest {

    private final InputValidationService service = new InputValidationService();

    @Test
    void rejectsRegistrationWhenRequiredFieldsAreMissing() {
        InputValidationException ex = assertThrows(InputValidationException.class,
                () -> service.validateComplaintInput(" ", " ", null, null, null, " ", null));
        assertEquals("Customer name is required", ex.getErrors().get("name"));
        assertEquals("Phone number is required", ex.getErrors().get("phone"));
        assertEquals("Account number is required", ex.getErrors().get("accountNumber"));
        assertEquals("Complaint description is required", ex.getErrors().get("description"));
    }

    @Test
    void rejectsScriptAndAcceptsACompleteRegistration() {
        InputValidationException ex = assertThrows(InputValidationException.class,
                () -> service.validateComplaintInput("Abebe Kebede", "0911223344", "1234567890123", "Bole",
                        "Bole Branch", "<script>alert(1)</script> description", null));
        assertTrue(ex.getErrors().get("description").contains("forbidden"));

        service.validateComplaintInput("Abebe Kebede", "0911223344", "1234567890123", "Bole",
                "Bole Branch", "The ATM did not dispense cash.", "abebe@example.com");
    }

    @Test
    void publicConsentMustBeExplicit() {
        assertThrows(InputValidationException.class, () -> service.requirePublicConsent(null));
        assertThrows(InputValidationException.class, () -> service.requirePublicConsent(false));
        assertThrows(InputValidationException.class, () -> service.requirePublicConsent("false"));
        service.requirePublicConsent(true);
        service.requirePublicConsent("true");
    }
}
