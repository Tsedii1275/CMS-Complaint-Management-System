package com.dashenbank.cms.controller;

import com.dashenbank.cms.model.NbeComplianceReport;
import com.dashenbank.cms.service.NbeComplianceReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * NBE compliance report persistence API.
 *
 * <p>
 * Ticket numbers contain a slash (for example {@code DBC-004/2026-27}), so they
 * are carried in the request body or as a query parameter and never as a path
 * variable.
 */
@RestController
@RequestMapping("/api/nbe-compliance-reports")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class NbeComplianceReportController {

    private final NbeComplianceReportService service;

    @Autowired
    public NbeComplianceReportController(NbeComplianceReportService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<NbeComplianceReport>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/by-ticket")
    public ResponseEntity<NbeComplianceReport> getByTicket(@RequestParam String ticketNumber) {
        return service.findByTicketNumber(ticketNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping
    public ResponseEntity<Object> upsert(@RequestBody NbeComplianceReport payload, Principal principal) {
        String username = principal != null ? principal.getName() : "Admin";
        try {
            return ResponseEntity.ok(service.upsertByTicketNumber(payload, username));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping
    public ResponseEntity<Map<String, String>> deleteByTicket(@RequestParam String ticketNumber) {
        if (!service.deleteByTicketNumber(ticketNumber)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No stored NBE compliance report for ticket " + ticketNumber));
        }
        return ResponseEntity.ok(Map.of("message", "Stored NBE compliance report values removed for " + ticketNumber));
    }
}
