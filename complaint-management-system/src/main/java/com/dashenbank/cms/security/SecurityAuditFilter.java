package com.dashenbank.cms.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Writes SIEM-style {@code [API AUDIT]} JSON lines to application logs (Docker stdout).
 */
public class SecurityAuditFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SecurityAuditFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (uri == null || !uri.startsWith("/api/") || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        long started = System.currentTimeMillis();
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        try {
            filterChain.doFilter(request, response);
        } finally {
            log.info("[API AUDIT] {}", toJson(request, response, traceId, System.currentTimeMillis() - started));
        }
    }

    static String eventType(String method, String uri, int status) {
        String path = uri == null ? "" : uri;
        if ("POST".equalsIgnoreCase(method) && path.startsWith("/api/auth/login")) {
            return status < 400 ? "LOGIN_SUCCESS" : "LOGIN_FAILURE";
        }
        if ("POST".equalsIgnoreCase(method) && path.startsWith("/api/auth/logout")) {
            return "LOGOUT";
        }
        if (path.startsWith("/api/users")
                || path.startsWith("/api/admin")
                || path.startsWith("/api/audit/logs")
                || path.startsWith("/api/audit/analytics")
                || path.startsWith("/api/sla/config")
                || path.startsWith("/api/nbe-compliance-reports")
                || path.startsWith("/api/rca")
                || path.startsWith("/api/complainant-related-information")) {
            return "ADMIN_ACTIVITY";
        }
        return "API_AUDIT";
    }

    private static String toJson(HttpServletRequest request, HttpServletResponse response, String traceId,
            long durationMs) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String user = "anonymous";
        String role = "";
        if (auth != null && auth.isAuthenticated() && auth.getName() != null
                && !"anonymousUser".equals(auth.getName())) {
            user = auth.getName();
            role = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(","));
        }
        String authHeader = request.getHeader("Authorization");
        String authMasked = (authHeader != null && !authHeader.isBlank()) ? "Bearer...[REDACTED]" : "";
        String uri = request.getRequestURI();
        int status = response.getStatus();
        return "{\"type\":\"" + json(eventType(request.getMethod(), uri, status))
                + "\",\"traceId\":\"" + json(traceId)
                + "\",\"user\":\"" + json(user)
                + "\",\"role\":\"" + json(role)
                + "\",\"ip\":\"" + json(ClientIp.from(request))
                + "\",\"method\":\"" + json(request.getMethod())
                + "\",\"uri\":\"" + json(uri)
                + "\",\"status\":" + status
                + ",\"durationMs\":" + durationMs
                + ",\"authHeader\":\"" + json(authMasked) + "\"}";
    }

    private static String json(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
