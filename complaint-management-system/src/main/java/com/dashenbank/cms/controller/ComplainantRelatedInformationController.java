package com.dashenbank.cms.controller;

import com.dashenbank.cms.model.ComplainantRelatedInformation;
import com.dashenbank.cms.service.ComplainantRelatedInformationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/complainant-related-information")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class ComplainantRelatedInformationController {

    private final ComplainantRelatedInformationService service;

    @Autowired
    public ComplainantRelatedInformationController(ComplainantRelatedInformationService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Page<ComplainantRelatedInformation>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String district,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {

        Page<ComplainantRelatedInformation> result = service.searchAndFilter(search, status, category, district, page,
                size);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComplainantRelatedInformation> getById(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-ticket/{uniqueIdNo}")
    public ResponseEntity<ComplainantRelatedInformation> getByTicket(@PathVariable String uniqueIdNo) {
        return service.findByUniqueIdNo(uniqueIdNo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ComplainantRelatedInformation> update(
            @PathVariable Long id,
            @RequestBody ComplainantRelatedInformation updateData,
            Principal principal) {

        String username = principal != null ? principal.getName() : "Admin";
        try {
            ComplainantRelatedInformation updated = service.updateRecord(id, updateData, username);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        service.deleteRecord(id);
        return ResponseEntity.ok(Map.of("message", "Record deleted successfully"));
    }

    @PostMapping("/sync")
    public ResponseEntity<Map<String, String>> triggerSync() {
        service.syncAllExistingComplaints();
        return ResponseEntity.ok(Map.of("message", "Complainant related information sync triggered successfully"));
    }
}
