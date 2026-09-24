package com.dashenbank.cms.security.ldap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.naming.AuthenticationException;
import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
public class LdapDirectoryClient implements DirectoryOperations {

    private static final Logger log = LoggerFactory.getLogger(LdapDirectoryClient.class);
    private static final int ACCOUNTDISABLE = 0x0002;
    private static final String[] USER_ATTRS = {
            "sAMAccountName", "displayName", "cn", "mail", "title", "userAccountControl", "objectGUID", "memberOf",
            "userPrincipalName"
    };

    private final LdapProperties properties;

    public LdapDirectoryClient(LdapProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean configured() {
        return properties.isEnabled()
                && StringUtils.hasText(properties.getUrl())
                && StringUtils.hasText(properties.resolvedBindDn())
                && StringUtils.hasText(properties.getBindPassword());
    }

    @Override
    public DirectoryHealth health() {
        if (!properties.isEnabled()) {
            return new DirectoryHealth(false, properties.getUrl(), "LDAP_ENABLED=false");
        }
        if (!configured()) {
            return new DirectoryHealth(false, properties.getUrl(), "LDAP bind DN or password is not configured");
        }
        try {
            closeQuietly(serviceContext());
            return new DirectoryHealth(true, properties.getUrl(), "Bind succeeded");
        } catch (DirectoryUnavailableException e) {
            return new DirectoryHealth(false, properties.getUrl(), safeDetail(e));
        }
    }

    @Override
    public AdUserProfile authenticate(String username, String password) {
        if (!StringUtils.hasText(username) || password == null || password.isEmpty()) {
            throw new DirectoryAuthenticationException("Invalid username or password");
        }
        if (!configured()) {
            throw new DirectoryUnavailableException("LDAP is not fully configured");
        }
        FoundUser found = findEntry(username.trim());
        if (found == null) {
            throw new DirectoryAuthenticationException("Invalid username or password");
        }
        if (!found.profile().enabled()) {
            throw new DirectoryAccountDisabledException("Active Directory account is disabled");
        }
        bindAsUser(found.dn(), password);
        return found.profile();
    }

    private void bindAsUser(String userDn, String password) {
        DirContext userCtx = null;
        try {
            userCtx = openContext(userDn, password, properties.getUrl());
        } catch (CommunicationException e) {
            userCtx = openFallbackContext(userDn, password, e);
        } catch (AuthenticationException e) {
            throw new DirectoryAuthenticationException("Invalid username or password");
        } catch (NamingException e) {
            throw new DirectoryUnavailableException("Directory is unavailable", e);
        } finally {
            closeQuietly(userCtx);
        }
    }

    private DirContext openFallbackContext(String userDn, String password, CommunicationException primary)
            throws DirectoryUnavailableException {
        if (!StringUtils.hasText(properties.getFallbackUrl())) {
            throw new DirectoryUnavailableException("Directory is unavailable", primary);
        }
        try {
            return openContext(userDn, password, properties.getFallbackUrl().trim());
        } catch (AuthenticationException ex) {
            throw new DirectoryAuthenticationException("Invalid username or password");
        } catch (NamingException ex) {
            throw new DirectoryUnavailableException("Directory is unavailable", ex);
        }
    }

    private DirContext openContext(String principal, String credentials, String url) throws NamingException {
        return context(principal, credentials, url, true);
    }

    @Override
    public Optional<AdUserProfile> findBySamAccountName(String username) {
        if (!StringUtils.hasText(username) || !configured()) {
            return Optional.empty();
        }
        FoundUser found = findEntry(username.trim());
        return found == null ? Optional.empty() : Optional.of(found.profile());
    }

    @Override
    public List<AdUserProfile> searchDirectoryUsers(int maxResults) {
        if (!configured()) {
            return List.of();
        }
        DirContext ctx = serviceContext();
        try {
            SearchControls controls = new SearchControls();
            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            controls.setReturningAttributes(USER_ATTRS);
            controls.setCountLimit(Math.max(1, maxResults));
            String filter = "(&(objectClass=user)(objectCategory=person)(!(userAccountControl:1.2.840.113556.1.4.803:=2)))";
            NamingEnumeration<SearchResult> results = ctx.search(properties.searchBase(), filter, controls);
            List<AdUserProfile> profiles = new ArrayList<>();
            while (results.hasMore() && profiles.size() < maxResults) {
                SearchResult result = results.next();
                AdUserProfile profile = toProfile(result.getAttributes());
                if (profile != null && StringUtils.hasText(profile.samAccountName())) {
                    profiles.add(profile);
                }
            }
            results.close();
            return profiles;
        } catch (NamingException e) {
            throw new DirectoryUnavailableException("Directory search failed", e);
        } finally {
            closeQuietly(ctx);
        }
    }

    private FoundUser findEntry(String username) {
        DirContext ctx = serviceContext();
        try {
            SearchControls controls = new SearchControls();
            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            controls.setReturningAttributes(USER_ATTRS);
            controls.setCountLimit(1);
            String filter = properties.getUserSearchFilter().replace("{0}", escapeFilter(username));
            NamingEnumeration<SearchResult> results = ctx.search(properties.searchBase(), filter, controls);
            if (!results.hasMore()) {
                results.close();
                return null;
            }
            SearchResult result = results.next();
            results.close();
            AdUserProfile profile = toProfile(result.getAttributes());
            return new FoundUser(result.getNameInNamespace(), profile);
        } catch (NamingException e) {
            log.warn("LDAP user search failed: {}", safeDetail(e));
            throw new DirectoryUnavailableException("Directory search failed", e);
        } finally {
            closeQuietly(ctx);
        }
    }

    private DirContext serviceContext() {
        try {
            return context(properties.resolvedBindDn(), properties.getBindPassword(), properties.getUrl(), false);
        } catch (CommunicationException e) {
            if (StringUtils.hasText(properties.getFallbackUrl())) {
                try {
                    return context(properties.resolvedBindDn(), properties.getBindPassword(),
                            properties.getFallbackUrl().trim(), false);
                } catch (NamingException ex) {
                    throw new DirectoryUnavailableException("Directory is unavailable", ex);
                }
            }
            throw new DirectoryUnavailableException("Directory is unavailable", e);
        } catch (AuthenticationException e) {
            log.warn("LDAP service bind failed");
            throw new DirectoryUnavailableException("LDAP service bind failed");
        } catch (NamingException e) {
            throw new DirectoryUnavailableException("Directory is unavailable", e);
        }
    }

    private DirContext context(String principal, String credentials, String url, boolean userBind)
            throws NamingException {
        Hashtable<String, Object> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, url);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.REFERRAL, "ignore");
        env.put(Context.SECURITY_PRINCIPAL, principal);
        env.put(Context.SECURITY_CREDENTIALS, credentials);
        env.put("com.sun.jndi.ldap.connect.timeout", String.valueOf(Math.max(1000, properties.getConnectTimeoutMs())));
        env.put("com.sun.jndi.ldap.read.timeout", String.valueOf(Math.max(1000, properties.getReadTimeoutMs())));
        if (url != null && url.toLowerCase(Locale.ROOT).startsWith("ldaps://")) {
            env.put(Context.SECURITY_PROTOCOL, "ssl");
            env.put("java.naming.ldap.factory.socket", VerifyingSslSocketFactory.class.getName());
            System.setProperty(VerifyingSslSocketFactory.PEER_PROPERTY,
                    StringUtils.hasText(properties.getSslPeerName()) ? properties.getSslPeerName().trim() : "");
        }
        try {
            return new InitialDirContext(env);
        } finally {
            if (userBind) {
                env.remove(Context.SECURITY_CREDENTIALS);
            }
        }
    }

    private static AdUserProfile toProfile(Attributes attrs) throws NamingException {
        if (attrs == null) {
            return null;
        }
        String sam = first(attrs, "sAMAccountName");
        String display = first(attrs, "displayName");
        if (!StringUtils.hasText(display)) {
            display = first(attrs, "cn");
        }
        String mail = first(attrs, "mail");
        if (!StringUtils.hasText(mail)) {
            mail = first(attrs, "userPrincipalName");
        }
        String title = first(attrs, "title");
        boolean enabled = isEnabled(first(attrs, "userAccountControl"));
        String guid = objectGuidToString(binary(attrs, "objectGUID"));
        return new AdUserProfile(sam, guid, display, mail, title, enabled, memberOf(attrs));
    }

    private static boolean isEnabled(String userAccountControl) {
        if (!StringUtils.hasText(userAccountControl)) {
            return true;
        }
        try {
            int uac = Integer.parseInt(userAccountControl.trim());
            return (uac & ACCOUNTDISABLE) == 0;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    private static List<String> memberOf(Attributes attrs) throws NamingException {
        Attribute attr = attrs.get("memberOf");
        if (attr == null) {
            return List.of();
        }
        List<String> groups = new ArrayList<>();
        NamingEnumeration<?> values = attr.getAll();
        while (values.hasMore()) {
            Object value = values.next();
            if (value != null) {
                groups.add(value.toString());
            }
        }
        values.close();
        return groups;
    }

    private static String first(Attributes attrs, String name) throws NamingException {
        Attribute attr = attrs.get(name);
        if (attr == null || attr.size() == 0) {
            return null;
        }
        Object value = attr.get();
        return value == null ? null : value.toString();
    }

    private static byte[] binary(Attributes attrs, String name) throws NamingException {
        Attribute attr = attrs.get(name);
        if (attr == null || attr.size() == 0) {
            return null;
        }
        Object value = attr.get();
        return value instanceof byte[] bytes ? bytes : null;
    }

    static String objectGuidToString(byte[] guid) {
        if (guid == null) {
            return null;
        }
        if (guid.length != 16) {
            StringBuilder hex = new StringBuilder();
            for (byte b : guid) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        }
        return String.format("%02x%02x%02x%02x-%02x%02x-%02x%02x-%02x%02x-%02x%02x%02x%02x%02x%02x",
                guid[3] & 0xff, guid[2] & 0xff, guid[1] & 0xff, guid[0] & 0xff,
                guid[5] & 0xff, guid[4] & 0xff,
                guid[7] & 0xff, guid[6] & 0xff,
                guid[8] & 0xff, guid[9] & 0xff,
                guid[10] & 0xff, guid[11] & 0xff, guid[12] & 0xff, guid[13] & 0xff, guid[14] & 0xff, guid[15] & 0xff);
    }

    static String escapeFilter(String value) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\5c");
                case '*' -> sb.append("\\2a");
                case '(' -> sb.append("\\28");
                case ')' -> sb.append("\\29");
                case '\0' -> sb.append("\\00");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private static void closeQuietly(DirContext ctx) {
        if (ctx == null) {
            return;
        }
        try {
            ctx.close();
        } catch (NamingException ignored) {
            // ignore
        }
    }

    private static String safeDetail(Exception e) {
        String text = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        if (text.length() > 300) {
            return text.substring(0, 300);
        }
        return text;
    }

    private record FoundUser(String dn, AdUserProfile profile) {
    }
}
