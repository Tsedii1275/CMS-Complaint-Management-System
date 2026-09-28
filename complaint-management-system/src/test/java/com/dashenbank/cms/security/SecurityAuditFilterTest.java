package com.dashenbank.cms.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SecurityAuditFilterTest {

    @Test
    void classifiesAuthAndAdminEvents() {
        assertEquals("LOGIN_SUCCESS", SecurityAuditFilter.eventType("POST", "/api/auth/login", 200));
        assertEquals("LOGIN_FAILURE", SecurityAuditFilter.eventType("POST", "/api/auth/login", 401));
        assertEquals("LOGOUT", SecurityAuditFilter.eventType("POST", "/api/auth/logout", 200));
        assertEquals("ADMIN_ACTIVITY", SecurityAuditFilter.eventType("GET", "/api/users", 200));
        assertEquals("API_AUDIT", SecurityAuditFilter.eventType("GET", "/api/tasks", 200));
    }
}
