package com.dashenbank.cms.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory per-IP sliding window limiter for the public/auth abuse surface.
 */
public class ApiRateLimitFilter extends OncePerRequestFilter {

    private static final int WINDOW_SECONDS = 60;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return limitFor(request.getServletPath(), request.getMethod()) <= 0;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        int max = limitFor(request.getServletPath(), request.getMethod());
        String key = ClientIp.from(request) + "|" + request.getServletPath();
        long now = Instant.now().getEpochSecond();
        Window window = windows.compute(key, (ignored, existing) -> {
            if (existing == null || now - existing.startEpoch >= WINDOW_SECONDS) {
                return new Window(now);
            }
            existing.count.incrementAndGet();
            return existing;
        });
        response.setHeader("X-RateLimit-Limit", String.valueOf(max));
        if (window.count.get() > max) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"Too many requests. Please try again later.\",\"code\":\"RATE_LIMITED\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    static int limitFor(String path, String method) {
        if (path == null) {
            return 0;
        }
        if ("POST".equalsIgnoreCase(method) && "/api/auth/login".equals(path)) {
            return 10;
        }
        if ("POST".equalsIgnoreCase(method) && "/api/complaints/start".equals(path)) {
            return 20;
        }
        if (path.startsWith("/api/complaints/status")) {
            return 30;
        }
        if ("POST".equalsIgnoreCase(method) && (path.equals("/api/complaints/upload-evidence")
                || path.equals("/api/complaints/upload-audio")
                || path.equals("/api/attachments/upload"))) {
            return 10;
        }
        return 0;
    }

    private static final class Window {
        private final long startEpoch;
        private final AtomicInteger count = new AtomicInteger(1);

        private Window(long startEpoch) {
            this.startEpoch = startEpoch;
        }
    }
}
