package com.dashenbank.cms.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter ensuring all API responses return consistent, hardened security
 * headers
 * and no-cache controls for sensitive application data.
 */
public class SecurityHeadersFilter extends OncePerRequestFilter {

    /**
     * Scripts are same-origin files only. Style attributes stay allowed because the
     * React UI sets them in markup.
     */
    public static final String CONTENT_SECURITY_POLICY = "default-src 'self'; script-src 'self'; "
            + "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; "
            + "font-src 'self' data: https://fonts.gstatic.com; img-src 'self' data: blob:; "
            + "connect-src 'self' ws: wss:; frame-ancestors 'none'; object-src 'none'; "
            + "base-uri 'self'; form-action 'self'";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Strict-Transport-Security (HSTS)
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");

        // 2. X-Frame-Options (Clickjacking Protection)
        response.setHeader("X-Frame-Options", "DENY");

        // 3. X-Content-Type-Options (MIME-Sniffing Protection)
        response.setHeader("X-Content-Type-Options", "nosniff");

        // 4. Referrer-Policy
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // 5. Permissions-Policy (Disable unnecessary browser capabilities)
        response.setHeader("Permissions-Policy",
                "geolocation=(), camera=(), microphone=(), payment=(), usb=(), display-capture=()");

        // 6. Content-Security-Policy (CSP)
        response.setHeader("Content-Security-Policy", CONTENT_SECURITY_POLICY);

        // 7. Cache-Control for sensitive endpoints (login, complaints, status,
        // dashboard, user profiles)
        String uri = request.getRequestURI();
        if (uri != null && (uri.startsWith("/api/") || uri.contains("/login") || uri.contains("/complaints")
                || uri.contains("/status") || uri.contains("/users") || uri.contains("/dashboard")
                || uri.contains("/profile"))) {
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);
        }

        filterChain.doFilter(request, response);
    }
}
