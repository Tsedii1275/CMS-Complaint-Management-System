package com.dashenbank.cms.controller;

import com.dashenbank.cms.service.ComplaintCategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/complaint-categories")
public class ComplaintCategoryController {

    private final ComplaintCategoryService service;

    public ComplaintCategoryController(ComplaintCategoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<Map<String, Object>> active() {
        return service.activeCategories();
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<Map<String, Object>> all() {
        return service.allCategories();
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody(required = false) Map<String, Object> body) {
        try {
            Object raw = body == null ? null : body.get("name");
            String name = raw == null ? null : String.valueOf(raw);
            return ResponseEntity.status(HttpStatus.CREATED).body(service.create(name));
        } catch (ResponseStatusException ex) {
            return error(ex);
        }
    }

    @PutMapping("/{id}/active")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> setActive(@PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body) {
        try {
            Object raw = body == null ? null : body.get("active");
            Boolean active = raw instanceof Boolean value ? value : null;
            return ResponseEntity.ok(service.setActive(id, active));
        } catch (ResponseStatusException ex) {
            return error(ex);
        }
    }

    private static ResponseEntity<Map<String, Object>> error(ResponseStatusException ex) {
        String message = ex.getReason() == null ? "Request could not be completed." : ex.getReason();
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message", message));
    }
}
