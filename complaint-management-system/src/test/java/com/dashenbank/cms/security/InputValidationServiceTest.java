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
        service.validateComplaintInput("Abebe Kebede", "+12025550123", "1234567890123", "Bole",
                "Bole Branch", "The ATM did not dispense cash.", null);
        service.validateComplaintInput("Abebe Kebede", "+25377123456", "1234567890123", "Bole",
                "Bole Branch", "The ATM did not dispense cash.", null);

        InputValidationException shortForeign = assertThrows(InputValidationException.class,
                () -> service.validateComplaintInput("Abebe Kebede", "+1123", "1234567890123", "Bole",
                        "Bole Branch", "The ATM did not dispense cash.", null));
        assertEquals("Invalid phone number format", shortForeign.getErrors().get("phone"));
    }

    @Test
    void districtAndBranchAcceptLettersOnly() {
        service.validateComplaintInput("Abebe Kebede", "0911223344", "1234567890123", "East Addis",
                "Bole Branch", "The ATM did not dispense cash.", null);
        service.validateComplaintInput("Abebe Kebede", "0911223344", "1234567890123", "ቦሌ",
                "ቦሌ ቅርንጫፍ", "The ATM did not dispense cash.", null);

        InputValidationException digits = assertThrows(InputValidationException.class,
                () -> service.validateComplaintInput("Abebe Kebede", "0911223344", "1234567890123", "East 1",
                        "Bole", "The ATM did not dispense cash.", null));
        assertEquals("District may contain letters and spaces only", digits.getErrors().get("district"));

        InputValidationException markup = assertThrows(InputValidationException.class,
                () -> service.validateComplaintInput("Abebe Kebede", "0911223344", "1234567890123", "Bole",
                        "Bole<script>", "The ATM did not dispense cash.", null));
        assertEquals("Branch may contain letters and spaces only", markup.getErrors().get("branch"));
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
