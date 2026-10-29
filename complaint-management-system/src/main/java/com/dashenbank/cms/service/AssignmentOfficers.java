package com.dashenbank.cms.service;

import com.dashenbank.cms.model.Role;
import com.dashenbank.cms.model.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Customer Care manager assignment can name more than one officer.
 * Flowable still has one task assignee (the first person). The full list
 * is stored on the case so every selected officer can see and work it.
 */
public final class AssignmentOfficers {

    public static final String KEY_OFFICERS = "assignedOfficers";
    public static final String KEY_USERNAMES = "assignedOfficerUsernames";

    private AssignmentOfficers() {
    }

    public static List<String> distinctUsernames(Object rawUsernames, String singleFallback) {
        List<String> ordered = new ArrayList<>();
        addAll(ordered, rawUsernames);
        if (ordered.isEmpty() && singleFallback != null && !singleFallback.isBlank()) {
            addOne(ordered, singleFallback);
        }
        return List.copyOf(ordered);
    }

    public static boolean canReceiveCustomerCareCase(User user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        Role role = user.getRole();
        return role == Role.ROLE_CUSTOMER_CARE_OFFICER || role == Role.ROLE_CUSTOMER_CARE_TEAM_LEADER;
    }

    public static List<Map<String, String>> records(List<User> users) {
        List<Map<String, String>> rows = new ArrayList<>();
        if (users == null) {
            return rows;
        }
        for (User user : users) {
            if (user == null || user.getUsername() == null || user.getUsername().isBlank()) {
                continue;
            }
            Map<String, String> row = new LinkedHashMap<>();
            row.put("username", user.getUsername().trim());
            row.put("fullName", displayName(user));
            rows.add(row);
        }
        return rows;
    }

    public static boolean includesUsername(Map<String, Object> vars, String username) {
        if (username == null || username.isBlank() || vars == null) {
            return false;
        }
        String wanted = username.trim();
        for (String candidate : usernames(vars)) {
            if (wanted.equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }

    public static String displayName(User user) {
        if (user == null) {
            return "";
        }
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName().trim();
        }
        return user.getUsername() == null ? "" : user.getUsername().trim();
    }

    public static String joinedNames(List<User> users) {
        List<String> names = new ArrayList<>();
        for (User user : users) {
            names.add(displayName(user) + " (" + user.getUsername().trim() + ")");
        }
        return String.join(", ", names);
    }

    private static List<String> usernames(Map<String, Object> vars) {
        List<String> names = new ArrayList<>();
        addAll(names, vars.get(KEY_USERNAMES));
        Object officers = vars.get(KEY_OFFICERS);
        if (officers instanceof Collection<?> collection) {
            for (Object item : collection) {
                if (item instanceof Map<?, ?> map && map.get("username") != null) {
                    addOne(names, map.get("username").toString());
                }
            }
        }
        return names;
    }

    private static void addAll(List<String> ordered, Object raw) {
        if (raw instanceof Collection<?> collection) {
            for (Object item : collection) {
                if (item != null) {
                    addOne(ordered, item.toString());
                }
            }
            return;
        }
        if (raw != null) {
            for (String part : raw.toString().split(",")) {
                addOne(ordered, part);
            }
        }
    }

    private static void addOne(List<String> ordered, String raw) {
        if (raw == null) {
            return;
        }
        String username = raw.trim();
        if (username.isEmpty()) {
            return;
        }
        String key = username.toLowerCase(Locale.ROOT);
        for (String existing : ordered) {
            if (existing.toLowerCase(Locale.ROOT).equals(key)) {
                return;
            }
        }
        ordered.add(username);
    }
}
