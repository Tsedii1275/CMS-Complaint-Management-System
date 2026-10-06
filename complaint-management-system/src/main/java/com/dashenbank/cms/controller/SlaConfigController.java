package com.dashenbank.cms.controller;

import com.dashenbank.cms.model.HolidayCalendar;
import com.dashenbank.cms.model.SlaConfig;
import com.dashenbank.cms.service.BusinessHoursService;
import com.dashenbank.cms.service.SlaConfigService;
import com.dashenbank.cms.service.StageSlaLedgerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sla/config")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class SlaConfigController {

    private static final String KEY_MESSAGE = "message";

    private final SlaConfigService slaConfigService;
    private final BusinessHoursService businessHoursService;
    private final StageSlaLedgerService stageSlaLedgerService;

    @Autowired
    public SlaConfigController(SlaConfigService slaConfigService, BusinessHoursService businessHoursService,
            StageSlaLedgerService stageSlaLedgerService) {
        this.slaConfigService = slaConfigService;
        this.businessHoursService = businessHoursService;
        this.stageSlaLedgerService = stageSlaLedgerService;
    }

    @GetMapping
    public ResponseEntity<List<SlaConfig>> getAllSlaConfigs() {
        return ResponseEntity.ok(slaConfigService.getAllConfigs());
    }

    @GetMapping("/operating-hours")
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> getOperatingHours() {
        return ResponseEntity.ok(businessHoursService.getOfficialOperatingSchedule());
    }

    @PutMapping("/{id}")
    public ResponseEntity<SlaConfig> updateSlaConfig(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {
        Integer allowedMinutes = payload.get("allowedMinutes") != null
                ? Integer.parseInt(payload.get("allowedMinutes").toString())
                : null;
        String displayName = (String) payload.get("displayName");
        String description = (String) payload.get("description");

        SlaConfig updated = slaConfigService.updateConfig(id, allowedMinutes, displayName, description);
        stageSlaLedgerService.evaluateOpenEvents();
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetToDefaults() {
        slaConfigService.resetSlaConfigs();
        stageSlaLedgerService.evaluateOpenEvents();
        return ResponseEntity.ok(Map.of(KEY_MESSAGE, "SLA configurations successfully reset to Dashen Bank defaults."));
    }

    @PostMapping("/purge")
    public ResponseEntity<Map<String, String>> purgeAllData() {
        slaConfigService.purgeAllSlaData();
        return ResponseEntity.ok(Map.of(KEY_MESSAGE,
                "SLA operational data reset: stage events, breach logs, metrics, and audit trail. Policy matrix retained."));
    }

    @GetMapping("/holidays")
    public ResponseEntity<List<HolidayCalendar>> getAllHolidays() {
        return ResponseEntity.ok(slaConfigService.getAllHolidays());
    }

    @PostMapping("/holidays")
    public ResponseEntity<HolidayCalendar> addHoliday(@RequestBody Map<String, Object> payload) {
        LocalDate date = LocalDate.parse(payload.get("holidayDate").toString());
        String name = (String) payload.get("holidayName");
        String type = (String) payload.get("holidayType");
        return ResponseEntity.ok(slaConfigService.addHoliday(date, name, type));
    }

    @DeleteMapping("/holidays/{id}")
    public ResponseEntity<Map<String, String>> deleteHoliday(@PathVariable Long id) {
        slaConfigService.deleteHoliday(id);
        return ResponseEntity.ok(Map.of(KEY_MESSAGE, "Holiday removed."));
    }
}
