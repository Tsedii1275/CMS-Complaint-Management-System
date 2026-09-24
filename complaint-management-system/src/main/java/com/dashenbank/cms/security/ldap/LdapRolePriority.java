package com.dashenbank.cms.security.ldap;

/**
 * Order used by {@link AdRoleMappingService}. Default is job title first.
 * Changing this property does not require a new authentication architecture.
 */
public enum LdapRolePriority {
    TITLE_THEN_GROUP,
    GROUP_THEN_TITLE
}
