package com.dashenbank.cms.controller;

import com.dashenbank.cms.model.HolidayCalendar;
import com.dashenbank.cms.model.SlaConfig;
import com.dashenbank.cms.service.BusinessHoursService;
import com.dashenbank.cms.service.SlaConfigService;
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

    @Autowired
    public SlaConfigController(SlaConfigService slaConfigService, BusinessHoursService businessHoursService) {
        this.slaConfigService = slaConfigService;
        this.businessHoursService = businessHoursService;
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
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetToDefaults() {
        slaConfigService.resetSlaConfigs();
        return ResponseEntity.ok(Map.of(KEY_MESSAGE, "SLA configurations successfully reset to Dashen Bank defaults."));
    }

    @PostMapping("/purge")
    public ResponseEntity<Map<String, String>> purgeAllData() {
        slaConfigService.purgeAllSlaData();
        return ResponseEntity.ok(Map.of(KEY_MESSAGE,
                "All SLA data, breach logs, and metrics successfully purged. System reset to fresh state."));
    }

    // ─── Holiday Calendar Endpoints ───

    @GetMapping("/holidays")
    public ResponseEntity<List<HolidayCalendar>> getAllHolidays() {
        return ResponseEntity.ok(slaConfigService.getAllHolidays());
    }

    @PostMapping("/holidays")
    public ResponseEntity<HolidayCalendar> addHoliday(@RequestBody Map<String, String> payload) {
        LocalDate date = LocalDate.parse(payload.get("holidayDate"));
        String name = payload.get("holidayName");
        String type = payload.get("holidayType");

        HolidayCalendar created = slaConfigService.addHoliday(date, name, type);
        return ResponseEntity.ok(created);
    }

    @DeleteMapping("/holidays/{id}")
    public ResponseEntity<Map<String, String>> deleteHoliday(@PathVariable Long id) {
        slaConfigService.deleteHoliday(id);
        return ResponseEntity.ok(Map.of(KEY_MESSAGE, "Holiday successfully removed."));
    }
}