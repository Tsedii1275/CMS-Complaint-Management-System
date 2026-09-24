package com.dashenbank.cms.security.ldap;

public record DirectoryHealth(boolean reachable, String urlUsed, String detail) {
}
