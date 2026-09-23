package com.dashenbank.cms.controller;

import com.dashenbank.cms.dto.CustomerProfileResponse;
import com.dashenbank.cms.integration.corebanking.CbsCustomerProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@PreAuthorize("isAuthenticated()")
public class CustomerProfileController {

    private static final ZoneId SYSTEM_ZONE = ZoneId.of("Africa/Addis_Ababa");

    private final CbsCustomerProfileService cbsCustomerProfileService;

    public CustomerProfileController(CbsCustomerProfileService cbsCustomerProfileService) {
        this.cbsCustomerProfileService = cbsCustomerProfileService;
    }

    @GetMapping({"/api/customer-profile/{accountNumber}", "/api/customers/by-account/{accountNumber}"})
    public ResponseEntity<Object> findByAccountNumber(@PathVariable String accountNumber) {
        if (accountNumber == null || !accountNumber.trim().matches("\\d{13}")) {
            return ResponseEntity.badRequest()
                    .body(errorBody("INVALID_ACCOUNT_NUMBER", "Account number must be 13 digits"));
        }
        return cbsCustomerProfileService.findByAccountNumber(accountNumber.trim())
                .<ResponseEntity<Object>>map(profile -> ResponseEntity.ok(CustomerProfileResponse.from(profile)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(errorBody("CUSTOMER_NOT_FOUND", "No customer found for this account number")));
    }

    private static Map<String, Object> errorBody(String code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("code", code);
        body.put("message", message);
        body.put("error", message);
        body.put("timestamp", LocalDateTime.now(SYSTEM_ZONE).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return body;
    }
}
