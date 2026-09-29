package com.dashenbank.cms.security.ldap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdOrganizationService {

    private static final Logger log = LoggerFactory.getLogger(AdOrganizationService.class);

    private final DirectoryOperations directory;
    private final LdapProperties properties;

    public AdOrganizationService(DirectoryOperations directory, LdapProperties properties) {
        this.directory = directory;
        this.properties = properties;
    }

    public List<AdOrgUnit> districts() {
        return unitsFor(AdAssignmentScope.DISTRICT);
    }

    public List<AdOrgUnit> branches() {
        return unitsFor(AdAssignmentScope.BRANCH);
    }

    public List<AdOrgUnit> departments() {
        return unitsFor(AdAssignmentScope.HEAD_OFFICE_DEPARTMENT);
    }

    public List<AdOrgOfficer> districtOfficers(String districtId) {
        return officersFor(AdAssignmentScope.DISTRICT, districtId);
    }

    public List<AdOrgOfficer> branchManagers(String branchId) {
        return officersFor(AdAssignmentScope.BRANCH, branchId);
    }

    public List<AdOrgOfficer> departmentLeaders(String departmentId) {
        return officersFor(AdAssignmentScope.HEAD_OFFICE_DEPARTMENT, departmentId);
    }

    List<AdOrgUnit> unitsFrom(List<AdUserProfile> profiles, AdAssignmentScope scope) {
        Map<String, String> byId = new LinkedHashMap<>();
        for (AdUserProfile profile : profiles) {
            if (matchesScope(profile, scope)) {
                String name = orgUnitName(profile, scope);
                if (StringUtils.hasText(name)) {
                    byId.putIfAbsent(idFor(name), name.trim());
                }
            }
        }
        List<AdOrgUnit> units = new ArrayList<>();
        byId.forEach((id, name) -> units.add(new AdOrgUnit(id, name)));
        units.sort(Comparator.comparing(AdOrgUnit::name, String.CASE_INSENSITIVE_ORDER));
        return units;
    }

    List<AdOrgOfficer> officersFrom(List<AdUserProfile> profiles, AdAssignmentScope scope, String unitId) {
        String expectedName = nameFromId(unitId);
        List<AdOrgOfficer> officers = new ArrayList<>();
        for (AdUserProfile profile : profiles) {
            if (isOfficerInUnit(profile, scope, expectedName)) {
                String display = StringUtils.hasText(profile.displayName()) ? profile.displayName()
                        : profile.samAccountName();
                officers.add(new AdOrgOfficer(profile.samAccountName(), display,
                        profile.title() == null ? "" : profile.title().trim()));
            }
        }
        officers.sort(Comparator.comparing(AdOrgOfficer::displayName, String.CASE_INSENSITIVE_ORDER));
        return officers;
    }

    static boolean matchesScope(AdUserProfile profile, AdAssignmentScope scope) {
        if (profile == null || !profile.enabled()) {
            return false;
        }
        String unit = orgUnitName(profile, scope);
        if (!StringUtils.hasText(unit)) {
            return false;
        }
        String lowerUnit = unit.toLowerCase(java.util.Locale.ROOT);
        String normalizedTitle = AdWorkUnitTitleMatcher.normalize(profile.title());

        if (scope == AdAssignmentScope.HEAD_OFFICE_DEPARTMENT) {
            if (lowerUnit.contains("branch") || lowerUnit.contains("district") || lowerUnit.contains("region")) {
                return false;
            }
            if (AdWorkUnitTitleMatcher.isBranchTitle(normalizedTitle)
                    || AdWorkUnitTitleMatcher.isDistrictTitle(normalizedTitle)) {
                return false;
            }
            return AdWorkUnitTitleMatcher.isHeadOfficeTitle(normalizedTitle)
                    || lowerUnit.contains("department")
                    || lowerUnit.contains("directorate")
                    || lowerUnit.contains("division")
                    || lowerUnit.contains("office")
                    || lowerUnit.contains("unit")
                    || lowerUnit.contains("center")
                    || lowerUnit.contains("centre")
                    || AdWorkUnitTitleMatcher.matchesGroup(profile.memberOf(), scope);
        }

        if (scope == AdAssignmentScope.BRANCH) {
            if (lowerUnit.contains("district") || lowerUnit.contains("region")) {
                return false;
            }
            if ((lowerUnit.contains("department") || lowerUnit.contains("directorate")
                    || lowerUnit.contains("division"))
                    && !lowerUnit.contains("branch")) {
                return false;
            }
            if (AdWorkUnitTitleMatcher.isDistrictTitle(normalizedTitle)) {
                return false;
            }
            return lowerUnit.contains("branch")
                    || AdWorkUnitTitleMatcher.isBranchTitle(normalizedTitle)
                    || AdWorkUnitTitleMatcher.matchesGroup(profile.memberOf(), scope);
        }

        if (scope == AdAssignmentScope.DISTRICT) {
            if (lowerUnit.contains("branch")) {
                return false;
            }
            return lowerUnit.contains("district")
                    || lowerUnit.contains("region")
                    || AdWorkUnitTitleMatcher.isDistrictTitle(normalizedTitle)
                    || AdWorkUnitTitleMatcher.matchesGroup(profile.memberOf(), scope);
        }

        return false;
    }

    static String orgUnitName(AdUserProfile profile, AdAssignmentScope scope) {
        if (profile == null) {
            return "";
        }
        String office = blankToEmpty(profile.office());
        String ou = ouFromDn(profile.distinguishedName());
        String department = blankToEmpty(profile.department());

        if (scope == AdAssignmentScope.HEAD_OFFICE_DEPARTMENT) {
            String name = firstNonBlank(department, ou, office);
            String lower = name.toLowerCase(java.util.Locale.ROOT);
            if (lower.contains("branch") || lower.contains("district") || lower.contains("region")) {
                return "";
            }
            return name;
        }

        if (scope == AdAssignmentScope.BRANCH) {
            String name = firstNonBlank(office, ou, department);
            String lower = name.toLowerCase(java.util.Locale.ROOT);
            if (lower.contains("district") || lower.contains("region")) {
                return "";
            }
            if ((lower.contains("department") || lower.contains("directorate") || lower.contains("division"))
                    && !lower.contains("branch")) {
                return "";
            }
            return name;
        }

        String name = firstNonBlank(office, ou, department);
        String lower = name.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("branch")) {
            return "";
        }
        return name;
    }

    static String orgUnitName(AdUserProfile profile) {
        return orgUnitName(profile, AdAssignmentScope.BRANCH);
    }

    static String ouFromDn(String distinguishedName) {
        if (!StringUtils.hasText(distinguishedName)) {
            return "";
        }
        for (String part : distinguishedName.split(",")) {
            String ou = parseAssignableOu(part);
            if (StringUtils.hasText(ou)) {
                return ou;
            }
        }
        return "";
    }

    private static String parseAssignableOu(String part) {
        String trimmed = part == null ? "" : part.trim();
        if (trimmed.length() < 4 || !trimmed.regionMatches(true, 0, "OU=", 0, 3)) {
            return "";
        }
        String ou = trimmed.substring(3).trim();
        if (ou.isEmpty() || "users".equalsIgnoreCase(ou) || "builtin".equalsIgnoreCase(ou)
                || "domain controllers".equalsIgnoreCase(ou)) {
            return "";
        }
        return ou;
    }

    private static boolean isOfficerInUnit(AdUserProfile profile, AdAssignmentScope scope, String expectedName) {
        if (!matchesScope(profile, scope) || !StringUtils.hasText(profile.samAccountName()) || expectedName == null) {
            return false;
        }
        String unit = orgUnitName(profile, scope);
        return StringUtils.hasText(unit) && expectedName.equalsIgnoreCase(unit.trim());
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private static String blankToEmpty(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    static String idFor(String name) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(name.trim().getBytes(StandardCharsets.UTF_8));
    }

    static String nameFromId(String id) {
        if (!StringUtils.hasText(id)) {
            return null;
        }
        try {
            return new String(Base64.getUrlDecoder().decode(id.trim()), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return id.trim();
        }
    }

    private List<AdOrgUnit> unitsFor(AdAssignmentScope scope) {
        return unitsFrom(directoryPeople(), scope);
    }

    private List<AdOrgOfficer> officersFor(AdAssignmentScope scope, String unitId) {
        return officersFrom(directoryPeople(), scope, unitId);
    }

    private List<AdUserProfile> directoryPeople() {
        if (!directory.configured()) {
            return List.of();
        }
        try {
            int pageSize = Math.max(100, properties.getSync().getPageSize());
            int maxTotal = Math.max(pageSize, properties.getSync().getAssignmentMaxResults());
            return directory.searchDirectoryUsersPaged(pageSize, maxTotal);
        } catch (RuntimeException e) {
            log.warn("AD organization search failed: {}", e.getMessage());
            throw e;
        }
    }
}
