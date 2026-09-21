package com.dashenbank.cms.controller;

import com.dashenbank.cms.integration.corebanking.CoreBankingClient;
import com.dashenbank.cms.integration.corebanking.CoreBankingProfile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
@PreAuthorize("isAuthenticated()")
public class CustomerController {

    private static final String KEY_ERROR = "error";

    private final CoreBankingClient coreBankingClient;

    public CustomerController(CoreBankingClient coreBankingClient) {
        this.coreBankingClient = coreBankingClient;
    }

    @GetMapping("/by-account/{accountNumber}")
    public ResponseEntity<Map<String, Object>> findByAccountNumber(@PathVariable String accountNumber) {
        if (accountNumber == null || !accountNumber.trim().matches("\\d{13}")) {
            return ResponseEntity.badRequest()
                    .body(Map.of(KEY_ERROR, "Account number must be 13 digits"));
        }
        return coreBankingClient.findByAccountNumber(accountNumber.trim())
                .map(profile -> ResponseEntity.ok(toResponse(profile)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(KEY_ERROR, "No customer found for this account number")));
    }

    private static Map<String, Object> toResponse(CoreBankingProfile profile) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("cifNumber", nullToEmpty(profile.cifNumber()));
        body.put("accountNumber", nullToEmpty(profile.accountNumber()));
        body.put("name", nullToEmpty(profile.name()));
        body.put("email", nullToEmpty(profile.email()));
        body.put("registeredPhone", nullToEmpty(profile.registeredPhone()));
        body.put("coreBankingPhone", nullToEmpty(profile.registeredPhone()));
        body.put("customerHomeBranch", nullToEmpty(profile.homeBranch()));
        body.put("customerHomeDistrict", nullToEmpty(profile.district()));
        body.put("customerSegment", nullToEmpty(profile.customerSegment()));
        body.put("customerSubSegment", nullToEmpty(profile.customerSubSegment()));
        return body;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
