package com.dashenbank.cms.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiRateLimitFilterTest {

    @Test
    void limitsPublicAbuseSurfaces() {
        assertEquals(10, ApiRateLimitFilter.limitFor("/api/auth/login", "POST"));
        assertEquals(20, ApiRateLimitFilter.limitFor("/api/complaints/start", "POST"));
        assertEquals(30, ApiRateLimitFilter.limitFor("/api/complaints/status", "GET"));
        assertEquals(30, ApiRateLimitFilter.limitFor("/api/complaints/status/CM-1", "GET"));
        assertEquals(10, ApiRateLimitFilter.limitFor("/api/complaints/upload-evidence", "POST"));
        assertEquals(10, ApiRateLimitFilter.limitFor("/api/complaints/upload-audio", "POST"));
        assertEquals(10, ApiRateLimitFilter.limitFor("/api/attachments/upload", "POST"));
    }

    @Test
    void doesNotLimitUnrelatedPaths() {
        assertEquals(0, ApiRateLimitFilter.limitFor("/api/tasks", "GET"));
        assertEquals(0, ApiRateLimitFilter.limitFor("/api/auth/login", "GET"));
        assertTrue(ApiRateLimitFilter.limitFor(null, "POST") == 0);
    }
}
