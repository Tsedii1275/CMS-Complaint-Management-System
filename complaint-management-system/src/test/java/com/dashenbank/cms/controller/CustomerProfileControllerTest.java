package com.dashenbank.cms.controller;

import com.dashenbank.cms.dto.CustomerProfileResponse;
import com.dashenbank.cms.exception.CbsException;
import com.dashenbank.cms.integration.corebanking.CbsCustomerProfileService;
import com.dashenbank.cms.integration.corebanking.CoreBankingProfile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerProfileControllerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private CbsCustomerProfileService cbsCustomerProfileService;
    private CustomerProfileController controller;

    @BeforeEach
    void setUp() {
        cbsCustomerProfileService = mock(CbsCustomerProfileService.class);
        controller = new CustomerProfileController(cbsCustomerProfileService);
    }

    @Test
    void rejectsNonThirteenDigitAccount() {
        ResponseEntity<Object> response = controller.findByAccountNumber("12345");
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void notFoundWhenCbsHasNoRow() {
        when(cbsCustomerProfileService.findByAccountNumber("5555666677778")).thenReturn(Optional.empty());
        ResponseEntity<Object> response = controller.findByAccountNumber("5555666677778");
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void returnsOnlyApprovedProfileFields() throws Exception {
        when(cbsCustomerProfileService.findByAccountNumber("5555666677778")).thenReturn(Optional.of(
                new CoreBankingProfile("Abebe Kebede", "0912345678", "Bole", "Addis District", "Retail")));
        ResponseEntity<Object> response = controller.findByAccountNumber("5555666677778");
        assertEquals(HttpStatus.OK, response.getStatusCode());

        CustomerProfileResponse body = (CustomerProfileResponse) response.getBody();
        assertEquals("Abebe Kebede", body.getCustomerName());
        assertEquals("0912345678", body.getPhoneNumber());
        assertEquals("Bole", body.getHomeBranch());
        assertEquals("Addis District", body.getHomeDistrict());
        assertEquals("Retail", body.getCustomerSegment());

        JsonNode json = OBJECT_MAPPER.readTree(OBJECT_MAPPER.writeValueAsString(body));
        Set<String> fields = new HashSet<>();
        json.fieldNames().forEachRemaining(fields::add);
        assertEquals(Set.of("customerName", "phoneNumber", "homeBranch", "homeDistrict", "customerSegment"), fields);
        assertFalse(json.has("cifNumber"));
        assertFalse(json.has("accountNumber"));
        assertFalse(json.has("email"));
        assertFalse(json.has("customerSubSegment"));
    }

    @Test
    void propagatesCbsUnavailable() {
        when(cbsCustomerProfileService.findByAccountNumber("5555666677778")).thenThrow(CbsException.unavailable());
        assertThrows(CbsException.class, () -> controller.findByAccountNumber("5555666677778"));
    }
}
