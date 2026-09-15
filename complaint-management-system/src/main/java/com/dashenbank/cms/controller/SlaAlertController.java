package com.dashenbank.cms.controller;

import com.dashenbank.cms.service.SlaAlertAuthorizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sla")
public class SlaAlertController {

    private final SlaAlertAuthorizationService slaAlertAuthorizationService;

    public SlaAlertController(SlaAlertAuthorizationService slaAlertAuthorizationService) {
        this.slaAlertAuthorizationService = slaAlertAuthorizationService;
    }

    @GetMapping("/alerts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> listAlerts() {
        return ResponseEntity.ok(slaAlertAuthorizationService.listVisibleAlerts());
    }

    @GetMapping("/alerts/complaint/{complaintId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> listAlertsForComplaint(@PathVariable String complaintId) {
        slaAlertAuthorizationService.assertCanViewComplaintSla(complaintId);
        return ResponseEntity.ok(slaAlertAuthorizationService.listVisibleAlerts().stream()
                .filter(alert -> complaintId.equalsIgnoreCase(String.valueOf(alert.get("complaintId")))
                        || complaintId.equalsIgnoreCase(String.valueOf(alert.get("dbcTicketId"))))
                .toList());
    }
}
