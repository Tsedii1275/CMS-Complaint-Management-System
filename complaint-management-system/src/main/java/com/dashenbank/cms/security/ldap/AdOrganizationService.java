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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
            if (matchesConfiguredScope(profile, scope)) {
                String name = orgUnitName(profile, scope);
                if (StringUtils.hasText(name)) {
                    byId.putIfAbsent(idFor(name), name.trim());
                }
            }
        }
        List<AdOrgUnit> units = new ArrayList<>();
        byId.forEach((id, name) -> units.add(new AdOrgUnit(id, name)));
        units.sort(Comparator.comparing(u -> u.name() != null ? u.name() : "", String.CASE_INSENSITIVE_ORDER));
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
        officers.sort(Comparator.comparing(o -> o.displayName() != null ? o.displayName() : "",
                String.CASE_INSENSITIVE_ORDER));
        return officers;
    }

    boolean matchesConfiguredScope(AdUserProfile profile, AdAssignmentScope scope) {
        return matchesScope(profile, scope, extras(scope));
    }

    static boolean matchesScope(AdUserProfile profile, AdAssignmentScope scope) {
        return matchesScope(profile, scope, List.of());
    }

    static boolean matchesScope(AdUserProfile profile, AdAssignmentScope scope, java.util.Collection<String> extraTitles) {
        if (profile == null || !profile.enabled()) {
            return false;
        }
        String unit = orgUnitName(profile, scope);
        if (!StringUtils.hasText(unit)) {
            return false;
        }
        String lowerUnit = unit.toLowerCase(Locale.ROOT);
        String normalizedTitle = AdWorkUnitTitleMatcher.normalize(profile.title());
        java.util.Collection<String> extras = extraTitles == null ? List.of() : extraTitles;

        if (scope == AdAssignmentScope.HEAD_OFFICE_DEPARTMENT) {
            if (AdBranchCatalog.looksLikeBranchUnit(unit) || lowerUnit.contains("district")
                    || lowerUnit.contains("region")) {
                return false;
            }
            if (AdWorkUnitTitleMatcher.isExclusiveBranchLeadership(normalizedTitle)
                    || AdWorkUnitTitleMatcher.isDistrictTitle(normalizedTitle)) {
                return false;
            }
            return AdWorkUnitTitleMatcher.matchesTitle(profile.title(), scope, unit, extras)
                    || AdWorkUnitTitleMatcher.matchesGroup(profile.memberOf(), scope, extras);
        }

        if (scope == AdAssignmentScope.BRANCH) {
            if (AdWorkUnitTitleMatcher.isDistrictTitle(normalizedTitle)) {
                return false;
            }
            if (AdBranchCatalog.isHeadOfficeIfbDepartment(unit)) {
                return false;
            }
            return AdWorkUnitTitleMatcher.matchesTitle(profile.title(), scope, unit, extras)
                    || AdWorkUnitTitleMatcher.matchesGroup(profile.memberOf(), scope, extras);
        }

        if (scope == AdAssignmentScope.DISTRICT) {
            if (AdBranchCatalog.looksLikeBranchUnit(unit)) {
                return false;
            }
            return AdWorkUnitTitleMatcher.matchesTitle(profile.title(), scope, unit, extras)
                    || AdWorkUnitTitleMatcher.matchesGroup(profile.memberOf(), scope, extras)
                    || lowerUnit.contains("district")
                    || lowerUnit.contains("region");
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
            if (AdBranchCatalog.looksLikeBranchUnit(name) || lower.contains("district") || lower.contains("region")) {
                return "";
            }
            return name;
        }

        if (scope == AdAssignmentScope.BRANCH) {
            String name = firstNonBlank(office, ou, department);
            String lower = name.toLowerCase(Locale.ROOT);
            if (lower.contains("district") || lower.contains("region")) {
                return "";
            }
            if (AdBranchCatalog.isHeadOfficeIfbDepartment(name)) {
                return "";
            }
            if ((lower.contains("department") || lower.contains("directorate") || lower.contains("division"))
                    && !lower.contains("branch") && !AdBranchCatalog.isIfbBranchWindow(name)) {
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
        if (ou.isEmpty() || AdBranchCatalog.isContainerOu(ou.toLowerCase(Locale.ROOT))) {
            return "";
        }
        return ou;
    }

    private boolean isOfficerInUnit(AdUserProfile profile, AdAssignmentScope scope, String expectedName) {
        if (!matchesConfiguredScope(profile, scope) || !StringUtils.hasText(profile.samAccountName())
                || expectedName == null) {
            return false;
        }
        String unit = orgUnitName(profile, scope);
        return AdOrgUnitMatcher.sameUnit(unit, expectedName);
    }

    private List<String> extras(AdAssignmentScope scope) {
        return properties == null ? List.of() : properties.extraTitlesFor(scope);
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

    /**
     * Reloads the branch master (AD OUs + people-derived names) and logs gaps.
     * Called after LDAP user sync so missing IFB/conventional OUs surface in logs.
     */
    public List<AdOrgUnit> validateBranchCatalog() {
        return unitsFor(AdAssignmentScope.BRANCH);
    }

    private List<AdOrgUnit> unitsFor(AdAssignmentScope scope) {
        List<AdUserProfile> people = directoryPeople();
        List<AdOrgUnit> fromPeople = unitsFrom(people, scope);
        if (scope != AdAssignmentScope.BRANCH) {
            return fromPeople;
        }
        List<String> masterNames = directoryOrganizationalUnits();
        List<AdOrgUnit> merged = mergeBranchMaster(masterNames, fromPeople);
        logBranchCatalogSync(masterNames, fromPeople, merged, people);
        return merged;
    }

    private List<String> directoryOrganizationalUnits() {
        if (!directory.configured()) {
            return List.of();
        }
        try {
            int max = Math.max(500, properties.getSync().getAssignmentMaxResults());
            return directory.searchOrganizationalUnitNames(max);
        } catch (RuntimeException e) {
            log.warn("AD branch OU search failed; falling back to people-derived names: {}", e.getMessage());
            return List.of();
        }
    }

    static List<AdOrgUnit> mergeBranchMaster(List<String> masterNames, List<AdOrgUnit> peopleUnits) {
        Map<String, String> byId = new LinkedHashMap<>();
        if (masterNames != null) {
            for (String name : masterNames) {
                if (!AdBranchCatalog.looksLikeBranchUnit(name)) {
                    continue;
                }
                String trimmed = name.trim();
                byId.putIfAbsent(idFor(trimmed), trimmed);
            }
        }
        if (peopleUnits != null) {
            for (AdOrgUnit unit : peopleUnits) {
                if (unit == null || !StringUtils.hasText(unit.name())) {
                    continue;
                }
                byId.putIfAbsent(unit.id(), unit.name().trim());
            }
        }
        List<AdOrgUnit> units = new ArrayList<>();
        byId.forEach((id, name) -> units.add(new AdOrgUnit(id, name)));
        units.sort(Comparator.comparing(u -> u.name() != null ? u.name() : "", String.CASE_INSENSITIVE_ORDER));
        return units;
    }

    private void logBranchCatalogSync(List<String> masterNames, List<AdOrgUnit> peopleUnits,
            List<AdOrgUnit> merged, List<AdUserProfile> people) {
        Set<String> masterFolded = foldedNames(masterNames);
        List<String> missingFromPeople = masterGaps(masterNames, foldedUnitNames(peopleUnits));
        List<String> extraInPeople = peopleOnlyGaps(peopleUnits, masterFolded);
        List<String> withoutOfficers = branchesWithoutOfficers(merged, people);
        log.info("Branch catalog sync: ouMaster={} peopleDerived={} merged={} missingPeopleRecords={} extraPeopleOnly={} withoutOfficers={}",
                masterNames == null ? 0 : masterNames.size(),
                peopleUnits == null ? 0 : peopleUnits.size(),
                merged.size(),
                missingFromPeople.size(),
                extraInPeople.size(),
                withoutOfficers.size());
        if (!missingFromPeople.isEmpty()) {
            log.warn("Branch master OUs with no matching enabled AD people (still listed for assignment): {}",
                    preview(missingFromPeople));
        }
        if (!withoutOfficers.isEmpty()) {
            log.warn("Branches with no assignable officer title: {}", preview(withoutOfficers));
        }
        if (!extraInPeople.isEmpty() && !masterFolded.isEmpty()) {
            log.info("People-derived branches not present as OUs: {}", preview(extraInPeople));
        }
    }

    private static List<String> masterGaps(List<String> masterNames, Set<String> peopleFolded) {
        List<String> missing = new ArrayList<>();
        for (String master : masterNames == null ? List.<String>of() : masterNames) {
            if (AdBranchCatalog.looksLikeBranchUnit(master)
                    && !peopleFolded.contains(master.trim().toLowerCase(Locale.ROOT))) {
                missing.add(master.trim());
            }
        }
        return missing;
    }

    private static List<String> peopleOnlyGaps(List<AdOrgUnit> peopleUnits, Set<String> masterFolded) {
        List<String> extra = new ArrayList<>();
        if (peopleUnits == null) {
            return extra;
        }
        for (AdOrgUnit unit : peopleUnits) {
            if (unit != null && StringUtils.hasText(unit.name())
                    && !masterFolded.contains(unit.name().trim().toLowerCase(Locale.ROOT))) {
                extra.add(unit.name().trim());
            }
        }
        return extra;
    }

    private List<String> branchesWithoutOfficers(List<AdOrgUnit> merged, List<AdUserProfile> people) {
        List<String> withoutOfficers = new ArrayList<>();
        for (AdOrgUnit unit : merged) {
            if (officersFrom(people, AdAssignmentScope.BRANCH, unit.id()).isEmpty()) {
                withoutOfficers.add(unit.name());
            }
        }
        return withoutOfficers;
    }

    private static Set<String> foldedUnitNames(List<AdOrgUnit> units) {
        Set<String> folded = new LinkedHashSet<>();
        if (units == null) {
            return folded;
        }
        for (AdOrgUnit unit : units) {
            if (unit != null && StringUtils.hasText(unit.name())) {
                folded.add(unit.name().trim().toLowerCase(Locale.ROOT));
            }
        }
        return folded;
    }

    private static Set<String> foldedNames(List<String> names) {
        Set<String> folded = new LinkedHashSet<>();
        if (names == null) {
            return folded;
        }
        for (String name : names) {
            if (StringUtils.hasText(name)) {
                folded.add(name.trim().toLowerCase(Locale.ROOT));
            }
        }
        return folded;
    }

    private static String preview(List<String> names) {
        int cap = Math.min(names.size(), 40);
        String shown = String.join(", ", names.subList(0, cap));
        if (names.size() > cap) {
            return shown + ", … +" + (names.size() - cap) + " more";
        }
        return shown;
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
