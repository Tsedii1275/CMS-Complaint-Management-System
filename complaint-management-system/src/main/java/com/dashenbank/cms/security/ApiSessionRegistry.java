package com.dashenbank.cms.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory API sessions for UAT evidence. Does not store JWT values.
 */
@Component
public class ApiSessionRegistry {

    private final ConcurrentHashMap<String, SessionSnapshot> sessions = new ConcurrentHashMap<>();

    public void opened(String username, String role, String ip, String userAgent) {
        if (username == null || username.isBlank()) {
            return;
        }
        sessions.put(username, new SessionSnapshot(username, role == null ? "" : role, Instant.now(),
                ip == null ? "" : ip, truncate(userAgent)));
    }

    public void closed(String username) {
        if (username != null) {
            sessions.remove(username);
        }
    }

    public List<SessionSnapshot> active() {
        return new ArrayList<>(sessions.values());
    }

    private static String truncate(String userAgent) {
        if (userAgent == null) {
            return "";
        }
        return userAgent.length() > 180 ? userAgent.substring(0, 180) : userAgent;
    }

    public record SessionSnapshot(String username, String role, Instant loginTime, String sourceIp, String userAgent) {
    }
}
