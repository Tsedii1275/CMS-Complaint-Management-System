package com.dashenbank.cms.security.ldap;

import com.dashenbank.cms.model.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AdRoleMappingService {

    private static final Logger log = LoggerFactory.getLogger(AdRoleMappingService.class);

    private final LdapProperties properties;

    public AdRoleMappingService(LdapProperties properties) {
        this.properties = properties;
    }

    public RoleResolution resolve(AdUserProfile profile) {
        if (profile == null) {
            return RoleResolution.none();
        }
        RoleResolution title = resolveFromTitle(profile.title());
        RoleResolution group = resolveFromGroups(profile.memberOf());
        LdapRolePriority priority = properties.getRolePriority();
        RoleResolution first = priority == LdapRolePriority.GROUP_THEN_TITLE ? group : title;
        RoleResolution second = priority == LdapRolePriority.GROUP_THEN_TITLE ? title : group;
        RoleResolution chosen = first.resolved() ? first : second;
        log.info("AD role resolution for {} priority={} titleMatch={} groupMatch={} resolved={} source={}",
                profile.samAccountName(), priority, title.resolved(), group.resolved(),
                chosen.role(), chosen.source());
        return chosen;
    }

    public RoleResolution resolveFromTitle(String title) {
        String normalized = normalize(title);
        if (normalized.isEmpty()) {
            return RoleResolution.none();
        }
        Map<String, Role> titles = titleMap();
        Role role = titles.get(normalized);
        if (role == null) {
            return RoleResolution.none();
        }
        return new RoleResolution(role, "JOB_TITLE", title.trim());
    }

    public RoleResolution resolveFromGroups(List<String> memberOf) {
        if (memberOf == null || memberOf.isEmpty()) {
            return RoleResolution.none();
        }
        Map<String, Role> groups = groupMap();
        if (groups.isEmpty()) {
            return RoleResolution.none();
        }
        for (String dn : memberOf) {
            String cn = groupKey(dn);
            Role role = groups.get(cn);
            if (role == null) {
                role = groups.get(normalize(dn));
            }
            if (role != null) {
                return new RoleResolution(role, "AD_GROUP", cn);
            }
        }
        return RoleResolution.none();
    }

    public Map<String, String> publishedTitleMappings() {
        Map<String, String> published = new LinkedHashMap<>();
        titleMap().forEach((title, role) -> published.put(title, role.name()));
        return published;
    }

    public Map<String, String> publishedGroupMappings() {
        Map<String, String> published = new LinkedHashMap<>();
        groupMap().forEach((group, role) -> published.put(group, role.name()));
        return published;
    }

    private Map<String, Role> titleMap() {
        Map<String, Role> map = defaultTitleMap();
        overlay(map, properties.parsedTitleMappings());
        return map;
    }

    private Map<String, Role> groupMap() {
        Map<String, Role> map = defaultGroupMap();
        overlay(map, properties.parsedGroupMappings());
        return map;
    }

    private static void overlay(Map<String, Role> target, Map<String, String> configured) {
        for (Map.Entry<String, String> entry : configured.entrySet()) {
            Role role = parseRole(entry.getValue());
            if (role != null && StringUtils.hasText(entry.getKey())) {
                target.put(normalize(entry.getKey()), role);
            }
        }
    }

    static Map<String, Role> defaultTitleMap() {
        Map<String, Role> map = new LinkedHashMap<>();
        putTitle(map, "Branch Manager", Role.ROLE_BRANCH_MANAGER);
        putTitle(map, "Customer Service Manager", Role.ROLE_CUSTOMER_SERVICE_MANAGER);
        putTitle(map, "Customer Care Officer", Role.ROLE_CUSTOMER_CARE_OFFICER);
        putTitle(map, "Customer Care Team Leader", Role.ROLE_CUSTOMER_CARE_TEAM_LEADER);
        putTitle(map, "Customer Care Senior Manager", Role.ROLE_CUSTOMER_CARE_SENIOR_MANAGER);
        putTitle(map, "Department / Work Unit Manager", Role.ROLE_DEPARTMENT_WORKUNIT);
        putTitle(map, "Work Unit Specialist", Role.ROLE_DEPARTMENT_WORKUNIT);
        putTitle(map, "Service Quality Director", Role.ROLE_SERVICE_QUALITY_DIRECTOR);
        putTitle(map, "Audit / Investigation Officer", Role.ROLE_AUDIT_INVESTIGATION_TEAM);
        putTitle(map, "Operation Audit Investigation Team", Role.ROLE_AUDIT_INVESTIGATION_TEAM);
        putTitle(map, "Contact Center Agent", Role.ROLE_CONTACT_CENTER_AGENT);
        putTitle(map, "Digital Marketing Officer", Role.ROLE_DIGITAL_MARKETING_OFFICER);
        putTitle(map, "Chief Experience Officer", Role.ROLE_CHIEF_EXPERIENCE_OFFICER);
        putTitle(map, "Contact Center Senior Manager", Role.ROLE_CONTACT_CENTER_SENIOR_MANAGER);
        putTitle(map, "Digital Marketing Senior Manager", Role.ROLE_DIGITAL_MARKETING_SENIOR_MANAGER);
        putTitle(map, "Customer Experience Partnership", Role.ROLE_CUSTOMER_EXPERIENCE_PARTNERSHIP);
        putTitle(map, "Operational Audit Senior Manager", Role.ROLE_OPERATIONAL_AUDIT_SENIOR_MANAGER);
        putTitle(map, "Operational Audit Director", Role.ROLE_OPERATIONAL_AUDIT_DIRECTOR);
        putTitle(map, "Committee Secretary", Role.ROLE_COMMITTEE_SECRETARY);
        return map;
    }

    static Map<String, Role> defaultGroupMap() {
        Map<String, Role> map = new LinkedHashMap<>();
        map.put(normalize("CMS_BRANCH_MANAGER"), Role.ROLE_BRANCH_MANAGER);
        map.put(normalize("CMS_CUSTOMER_SERVICE_MANAGER"), Role.ROLE_CUSTOMER_SERVICE_MANAGER);
        map.put(normalize("CMS_CUSTOMER_CARE_OFFICER"), Role.ROLE_CUSTOMER_CARE_OFFICER);
        map.put(normalize("CMS_CUSTOMER_CARE_TEAM_LEADER"), Role.ROLE_CUSTOMER_CARE_TEAM_LEADER);
        map.put(normalize("CMS_CUSTOMER_CARE_SENIOR_MANAGER"), Role.ROLE_CUSTOMER_CARE_SENIOR_MANAGER);
        map.put(normalize("CMS_DEPARTMENT_WORKUNIT"), Role.ROLE_DEPARTMENT_WORKUNIT);
        map.put(normalize("CMS_SERVICE_QUALITY_DIRECTOR"), Role.ROLE_SERVICE_QUALITY_DIRECTOR);
        map.put(normalize("CMS_AUDIT_INVESTIGATION_TEAM"), Role.ROLE_AUDIT_INVESTIGATION_TEAM);
        map.put(normalize("CMS_CONTACT_CENTER_AGENT"), Role.ROLE_CONTACT_CENTER_AGENT);
        map.put(normalize("CMS_DIGITAL_MARKETING_OFFICER"), Role.ROLE_DIGITAL_MARKETING_OFFICER);
        map.put(normalize("CMS_CHIEF_EXPERIENCE_OFFICER"), Role.ROLE_CHIEF_EXPERIENCE_OFFICER);
        map.put(normalize("CMS_CONTACT_CENTER_SENIOR_MANAGER"), Role.ROLE_CONTACT_CENTER_SENIOR_MANAGER);
        map.put(normalize("CMS_DIGITAL_MARKETING_SENIOR_MANAGER"), Role.ROLE_DIGITAL_MARKETING_SENIOR_MANAGER);
        map.put(normalize("CMS_CUSTOMER_EXPERIENCE_PARTNERSHIP"), Role.ROLE_CUSTOMER_EXPERIENCE_PARTNERSHIP);
        map.put(normalize("CMS_OPERATIONAL_AUDIT_SENIOR_MANAGER"), Role.ROLE_OPERATIONAL_AUDIT_SENIOR_MANAGER);
        map.put(normalize("CMS_OPERATIONAL_AUDIT_DIRECTOR"), Role.ROLE_OPERATIONAL_AUDIT_DIRECTOR);
        map.put(normalize("CMS_COMMITTEE_SECRETARY"), Role.ROLE_COMMITTEE_SECRETARY);
        map.put(normalize("CMS_ADMIN"), Role.ROLE_ADMIN);
        return map;
    }

    private static void putTitle(Map<String, Role> map, String title, Role role) {
        map.put(normalize(title), role);
    }

    static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    static String groupKey(String memberOfDn) {
        if (!StringUtils.hasText(memberOfDn)) {
            return "";
        }
        String dn = memberOfDn.trim();
        int comma = dn.indexOf(',');
        String first = comma >= 0 ? dn.substring(0, comma) : dn;
        if (first.regionMatches(true, 0, "CN=", 0, 3)) {
            return normalize(first.substring(3));
        }
        return normalize(first);
    }

    private static Role parseRole(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Role.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
