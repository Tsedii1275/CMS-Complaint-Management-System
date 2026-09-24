package com.dashenbank.cms.security.ldap;

public record SyncRunResult(int synced, int failed, String error) {
}
