package com.dashenbank.cms.security.ldap;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@ConfigurationProperties(prefix = "ldap")
public class LdapProperties {

    private boolean enabled = false;
    private String url = "";
    private String fallbackUrl = "";
    private String bindDn = "";
    private String bindPassword = "";
    private String baseDn = "DC=dashenbank,DC=local";
    private String userSearchBase = "";
    /** OU tree used as the branch master, e.g. {@code OU=Dashen Bank,DC=dashenbank,DC=local}. */
    private String groupSearchBase = "";
    private String userSearchFilter = "(&(objectClass=user)(sAMAccountName={0}))";
    private String domain = "dashenbank.local";
    /**
     * Certificate hostname to verify when {@link #url} uses an IP address.
     * This is still TLS hostname verification — not trust-all.
     */
    private String sslPeerName = "ldap.dashenbank.local";
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 10000;
    private LdapRolePriority rolePriority = LdapRolePriority.TITLE_THEN_GROUP;
    /** Semicolon map: {@code Branch Manager=ROLE_BRANCH_MANAGER;...} */
    private String titleMappings = "";
    /**
     * Semicolon map of group CN (not full DN):
     * {@code CMS_BRANCH_MANAGER=ROLE_BRANCH_MANAGER}
     */
    private String groupMappings = "";
    private final WorkUnit workUnit = new WorkUnit();
    private final Sync sync = new Sync();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFallbackUrl() {
        return fallbackUrl;
    }

    public void setFallbackUrl(String fallbackUrl) {
        this.fallbackUrl = fallbackUrl;
    }

    public String getBindDn() {
        return bindDn;
    }

    public void setBindDn(String bindDn) {
        this.bindDn = bindDn;
    }

    public String getBindPassword() {
        return bindPassword;
    }

    public void setBindPassword(String bindPassword) {
        this.bindPassword = bindPassword;
    }

    public String getBaseDn() {
        return baseDn;
    }

    public void setBaseDn(String baseDn) {
        this.baseDn = baseDn;
    }

    public String getUserSearchBase() {
        return userSearchBase;
    }

    public void setUserSearchBase(String userSearchBase) {
        this.userSearchBase = userSearchBase;
    }

    public String getGroupSearchBase() {
        return groupSearchBase;
    }

    public void setGroupSearchBase(String groupSearchBase) {
        this.groupSearchBase = groupSearchBase;
    }

    public String organizationSearchBase() {
        if (StringUtils.hasText(groupSearchBase)) {
            return groupSearchBase.trim();
        }
        return searchBase();
    }

    public String getUserSearchFilter() {
        return userSearchFilter;
    }

    public void setUserSearchFilter(String userSearchFilter) {
        this.userSearchFilter = userSearchFilter;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getSslPeerName() {
        return sslPeerName;
    }

    public void setSslPeerName(String sslPeerName) {
        this.sslPeerName = sslPeerName;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public LdapRolePriority getRolePriority() {
        return rolePriority == null ? LdapRolePriority.TITLE_THEN_GROUP : rolePriority;
    }

    public void setRolePriority(LdapRolePriority rolePriority) {
        this.rolePriority = rolePriority;
    }

    public String getTitleMappings() {
        return titleMappings;
    }

    public void setTitleMappings(String titleMappings) {
        this.titleMappings = titleMappings;
    }

    public String getGroupMappings() {
        return groupMappings;
    }

    public void setGroupMappings(String groupMappings) {
        this.groupMappings = groupMappings;
    }

    public Sync getSync() {
        return sync;
    }

    public String resolvedBindDn() {
        if (!StringUtils.hasText(bindDn)) {
            return "";
        }
        String trimmed = bindDn.trim();
        if (trimmed.contains("@")
                || trimmed.toUpperCase(Locale.ROOT).startsWith("CN=")
                || trimmed.contains("/")) {
            return toLdapDn(trimmed);
        }
        if (StringUtils.hasText(domain)) {
            return trimmed + "@" + domain.trim();
        }
        return trimmed;
    }

    public String searchBase() {
        if (StringUtils.hasText(userSearchBase)) {
            return userSearchBase.trim();
        }
        return StringUtils.hasText(baseDn) ? baseDn.trim() : "";
    }

    public Map<String, String> parsedTitleMappings() {
        return parseMap(titleMappings);
    }

    public Map<String, String> parsedGroupMappings() {
        return parseMap(groupMappings);
    }

    public WorkUnit getWorkUnit() {
        return workUnit;
    }

    /**
     * Extra title phrases for a Work Unit scope: configured lists plus
     * {@code ldap.title-mappings} keys whose CMS role belongs to that scope.
     */
    public List<String> extraTitlesFor(AdAssignmentScope scope) {
        List<String> extras = new ArrayList<>();
        extras.addAll(AdWorkUnitTitleMatcher.parseTitleList(workUnit.titlesRaw(scope)));
        for (Map.Entry<String, String> entry : parsedTitleMappings().entrySet()) {
            if (scope.equals(scopeForMappedRole(entry.getValue()))) {
                extras.add(entry.getKey());
            }
        }
        return extras;
    }

    static AdAssignmentScope scopeForMappedRole(String role) {
        if (!StringUtils.hasText(role)) {
            return null;
        }
        String folded = role.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        if (folded.contains("DISTRICT")) {
            return AdAssignmentScope.DISTRICT;
        }
        if (folded.contains("BRANCH_MANAGER") || folded.contains("CUSTOMER_SERVICE_MANAGER")
                || folded.contains("CUSTOMER_SERVICE") || folded.endsWith("_CSM") || folded.contains("_CSM_")) {
            return AdAssignmentScope.BRANCH;
        }
        if (folded.contains("DIRECTOR") || folded.contains("SENIOR_MANAGER") || folded.contains("DEPARTMENT")
                || folded.contains("HEAD_OFFICE") || folded.contains("DIVISION_HEAD")) {
            return AdAssignmentScope.HEAD_OFFICE_DEPARTMENT;
        }
        return null;
    }

    /**
     * Accepts a real DN or the bank Windows path
     * {@code dashenbank.local/Dashen Bank/.../CASHCOMP}.
     */
    static String toLdapDn(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.toUpperCase(Locale.ROOT).startsWith("CN=")
                || trimmed.toUpperCase(Locale.ROOT).startsWith("OU=")
                || trimmed.toUpperCase(Locale.ROOT).startsWith("DC=")) {
            return trimmed;
        }
        String[] parts = trimmed.split("/");
        if (parts.length < 2) {
            return trimmed;
        }
        String domain = parts[0].trim();
        StringBuilder dn = new StringBuilder();
        for (int i = parts.length - 1; i >= 1; i--) {
            String part = parts[i].trim();
            if (part.isEmpty()) {
                continue;
            }
            if (!dn.isEmpty()) {
                dn.append(',');
            }
            dn.append(i == parts.length - 1 ? "CN=" : "OU=").append(part);
        }
        for (String dc : domain.split("\\.")) {
            if (!dc.isBlank()) {
                dn.append(",DC=").append(dc.trim());
            }
        }
        return dn.toString();
    }

    static Map<String, String> parseMap(String raw) {
        Map<String, String> out = new LinkedHashMap<>();
        if (!StringUtils.hasText(raw)) {
            return out;
        }
        for (String entry : raw.split(";")) {
            String item = entry.trim();
            if (item.isEmpty()) {
                continue;
            }
            int eq = item.indexOf('=');
            if (eq <= 0 || eq == item.length() - 1) {
                continue;
            }
            out.put(item.substring(0, eq).trim(), item.substring(eq + 1).trim());
        }
        return out;
    }

    public static class WorkUnit {
        private String branchTitles = "";
        private String headOfficeTitles = "";
        private String districtTitles = "";

        public String getBranchTitles() {
            return branchTitles;
        }

        public void setBranchTitles(String branchTitles) {
            this.branchTitles = branchTitles;
        }

        public String getHeadOfficeTitles() {
            return headOfficeTitles;
        }

        public void setHeadOfficeTitles(String headOfficeTitles) {
            this.headOfficeTitles = headOfficeTitles;
        }

        public String getDistrictTitles() {
            return districtTitles;
        }

        public void setDistrictTitles(String districtTitles) {
            this.districtTitles = districtTitles;
        }

        String titlesRaw(AdAssignmentScope scope) {
            if (scope == AdAssignmentScope.BRANCH) {
                return branchTitles;
            }
            if (scope == AdAssignmentScope.HEAD_OFFICE_DEPARTMENT) {
                return headOfficeTitles;
            }
            if (scope == AdAssignmentScope.DISTRICT) {
                return districtTitles;
            }
            return "";
        }
    }

    public static class Sync {
        private boolean enabled = false;
        private String cron = "0 0 * * * *";
        private boolean discovery = false;
        private int maxResults = 500;
        private int pageSize = 500;
        private int assignmentMaxResults = 10_000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }

        public boolean isDiscovery() {
            return discovery;
        }

        public void setDiscovery(boolean discovery) {
            this.discovery = discovery;
        }

        public int getMaxResults() {
            return maxResults;
        }

        public void setMaxResults(int maxResults) {
            this.maxResults = maxResults;
        }

        public int getPageSize() {
            return pageSize <= 0 ? 500 : pageSize;
        }

        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }

        public int getAssignmentMaxResults() {
            return assignmentMaxResults <= 0 ? 10_000 : assignmentMaxResults;
        }

        public void setAssignmentMaxResults(int assignmentMaxResults) {
            this.assignmentMaxResults = assignmentMaxResults;
        }
    }
}
