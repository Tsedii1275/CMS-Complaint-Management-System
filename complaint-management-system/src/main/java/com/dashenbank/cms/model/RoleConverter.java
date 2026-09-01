package com.dashenbank.cms.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;

@Slf4j
@Converter(autoApply = true)
public class RoleConverter implements AttributeConverter<Role, String> {

    @Override
    public String convertToDatabaseColumn(Role role) {
        if (role == null) {
            log.error("Role integrity violation: role entity attribute is null.");
            throw new IllegalStateException("Invalid role configuration detected.");
        }
        return role.name();
    }

    @Override
    public Role convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            log.error("Role integrity violation: role value is null or empty.");
            throw new IllegalStateException("Invalid role configuration detected.");
        }

        String normalized = normalizeRoleToken(dbData);
        try {
            return Role.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return mapUnknownOrLegacyRole(dbData, normalized);
        }
    }

    private static Role mapUnknownOrLegacyRole(String dbData, String normalized) {
        if (isLegacyContactCenterManager(dbData, normalized)) {
            return Role.ROLE_CONTACT_CENTER_SENIOR_MANAGER;
        }
        if ("ROLE_SERVICE_QUALITY".equals(normalized)) {
            return Role.ROLE_CUSTOMER_CARE_OFFICER;
        }
        String legacy = dbData.trim().toUpperCase(Locale.ROOT);
        if (legacy.contains("CMD") || legacy.contains("CC_OFFICER") || legacy.contains("CUSTOMER_CARE")) {
            return Role.ROLE_CUSTOMER_CARE_OFFICER;
        }
        if (legacy.contains("CEX")) {
            return Role.ROLE_CHIEF_EXPERIENCE_OFFICER;
        }
        if (legacy.contains("AUDIT") && (legacy.contains("TEAM") || legacy.contains("OFFICER"))) {
            return Role.ROLE_AUDIT_INVESTIGATION_TEAM;
        }
        log.warn("Unknown or legacy role '{}' found in database. Mapping safely to ROLE_BRANCH_STAFF.", dbData);
        return Role.ROLE_BRANCH_STAFF;
    }

    private static String normalizeRoleToken(String dbData) {
        String clean = dbData.trim().toUpperCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
        if (!clean.startsWith("ROLE_")) {
            clean = "ROLE_" + clean;
        }
        return clean;
    }

    /**
     * Former ROLE_CONTACT_CENTER_MANAGER and display-name variants must never
     * fall through to Branch Staff after the enum constant was removed.
     */
    private static boolean isLegacyContactCenterManager(String original, String normalized) {
        if ("ROLE_CONTACT_CENTER_MANAGER".equals(normalized)) {
            return true;
        }
        String probe = original.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        boolean contactCenter = probe.contains("CONTACT_CENTER") || probe.contains("CONTACT CENTER".replace(' ', '_'));
        boolean manager = probe.contains("MANAGER");
        boolean senior = probe.contains("SENIOR");
        boolean agent = probe.contains("AGENT");
        return contactCenter && manager && !senior && !agent;
    }
}
