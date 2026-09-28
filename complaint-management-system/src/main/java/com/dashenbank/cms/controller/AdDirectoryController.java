package com.dashenbank.cms.controller;

import com.dashenbank.cms.security.ldap.AdOrganizationService;
import com.dashenbank.cms.security.ldap.DirectoryUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ad")
@PreAuthorize("isAuthenticated()")
public class AdDirectoryController {

    private final AdOrganizationService organizationService;

    public AdDirectoryController(AdOrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping("/districts")
    public ResponseEntity<?> districts() {
        return wrap(organizationService::districts);
    }

    @GetMapping("/districts/{districtId}/officers")
    public ResponseEntity<?> districtOfficers(@PathVariable String districtId) {
        return wrap(() -> organizationService.districtOfficers(districtId));
    }

    @GetMapping("/branches")
    public ResponseEntity<?> branches() {
        return wrap(organizationService::branches);
    }

    @GetMapping("/branches/{branchId}/managers")
    public ResponseEntity<?> branchManagers(@PathVariable String branchId) {
        return wrap(() -> organizationService.branchManagers(branchId));
    }

    @GetMapping("/departments")
    public ResponseEntity<?> departments() {
        return wrap(organizationService::departments);
    }

    @GetMapping("/departments/{departmentId}/leaders")
    public ResponseEntity<?> departmentLeaders(@PathVariable String departmentId) {
        return wrap(() -> organizationService.departmentLeaders(departmentId));
    }

    private static ResponseEntity<?> wrap(SupplierList supplier) {
        try {
            return ResponseEntity.ok(supplier.get());
        } catch (DirectoryUnavailableException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Active Directory is unavailable for work-unit assignment."));
        }
    }

    @FunctionalInterface
    private interface SupplierList {
        List<?> get();
    }
}
