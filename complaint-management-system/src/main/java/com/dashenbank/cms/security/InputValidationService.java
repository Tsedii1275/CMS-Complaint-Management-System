package com.dashenbank.cms.security;

import com.dashenbank.cms.exception.InputValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@Slf4j
public class InputValidationService {

    // 1. Customer Name: Letters (Latin + Ethiopic/Ge'ez), spaces, apostrophes,
    // hyphens (2 to 100 chars)
    private static final Pattern NAME_PATTERN = Pattern
            .compile("^[a-zA-Z\\u00C0-\\u024F\\u1200-\\u137F\\s'\\-]{2,100}$");

    // 2. Phone Number: Ethiopian phone numbers (+2519XXXXXXXX, +2517XXXXXXXX,
    // 09XXXXXXXX, 07XXXXXXXX)
    private static final Pattern ETHIOPIAN_PHONE_PATTERN = Pattern.compile("^(\\+251[79]\\d{8}|0[79]\\d{8})$");

    // 3. Account Number: Exactly 13 numeric digits
    private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile("^\\d{13}$");

    // 4 & 5. District & Branch: Max 100 chars, alphanumeric + Ge'ez + standard
    // punctuation, no HTML/JS/SQL
    private static final Pattern DISTRICT_BRANCH_PATTERN = Pattern
            .compile("^[a-zA-Z0-9\\u00C0-\\u024F\\u1200-\\u137F\\s'\\-_/.]{1,100}$");

    // Ticket & Task ID Validation
    private static final Pattern TICKET_ID_PATTERN = Pattern.compile("^[A-Za-z0-9/\\-_]{3,50}$");
    private static final Pattern TASK_ID_PATTERN = Pattern.compile("^[A-Za-z0-9/\\-_]{1,50}$");

    // XSS / Script / HTML Injection Detection Patterns
    private static final Pattern XSS_HTML_TAG_PATTERN = Pattern.compile(
            "(?i)<[^>]*script[^>]*>|<[^>]+on\\w+\\s*=|javascript:|data:text/html|<\\s*iframe|<\\s*object|<\\s*embed|<\\s*svg|<\\s*img|<\\s*link|<\\s*style|<\\s*meta");
    private static final Pattern ANY_HTML_TAG_PATTERN = Pattern.compile("(?i)<[/]?[a-z1-6]+(\\s+[^>]*)?>");

    /**
     * Primary validation for public & internal complaint registration inputs.
     */
    public void validateComplaintInput(String name, String phone, String accountNumber,
            String district, String branch, String description,
            String email) {
        Map<String, String> errors = new LinkedHashMap<>();

        // 1. Customer Name Validation
        if (name == null || name.isBlank()) {
            errors.put("name", "Customer name is required");
        } else {
            String trimmedName = name.trim();
            if (trimmedName.length() < 2 || trimmedName.length() > 100) {
                errors.put("name", "Customer name must be between 2 and 100 characters");
            } else if (!NAME_PATTERN.matcher(trimmedName).matches() || containsXssOrHtml(trimmedName)) {
                errors.put("name", "Customer name contains invalid characters");
            }
        }

        // 2. Phone Number Validation
        if (phone == null || phone.isBlank()) {
            errors.put("phone", "Phone number is required");
        } else {
            String trimmedPhone = phone.trim().replaceAll("\\s+", "");
            if (!ETHIOPIAN_PHONE_PATTERN.matcher(trimmedPhone).matches() || containsXssOrHtml(trimmedPhone)) {
                errors.put("phone", "Invalid Ethiopian phone number format");
            }
        }

        // 3. Account Number Validation (required on public and staff registration)
        if (accountNumber == null || accountNumber.isBlank()) {
            errors.put("accountNumber", "Account number is required");
        } else {
            String trimmedAccount = accountNumber.trim();
            if (!ACCOUNT_NUMBER_PATTERN.matcher(trimmedAccount).matches() || containsXssOrHtml(trimmedAccount)) {
                errors.put("accountNumber", "Account number must be exactly 13 numeric digits");
            }
        }

        // 4. District Validation
        if (district != null && !district.isBlank()) {
            String trimmedDistrict = district.trim();
            if (trimmedDistrict.length() > 100) {
                errors.put("district", "District name cannot exceed 100 characters");
            } else if (!DISTRICT_BRANCH_PATTERN.matcher(trimmedDistrict).matches()
                    || containsXssOrHtml(trimmedDistrict)) {
                errors.put("district", "District contains invalid characters or script tags");
            }
        }

        // 5. Branch Validation
        if (branch != null && !branch.isBlank()) {
            String trimmedBranch = branch.trim();
            if (trimmedBranch.length() > 100) {
                errors.put("branch", "Branch name cannot exceed 100 characters");
            } else if (!DISTRICT_BRANCH_PATTERN.matcher(trimmedBranch).matches() || containsXssOrHtml(trimmedBranch)) {
                errors.put("branch", "Branch contains invalid characters or script tags");
            }
        }

        // 6. Complaint Description Validation
        if (description == null || description.isBlank()) {
            errors.put("description", "Complaint description is required");
        } else {
            String trimmedDesc = description.trim();
            if (trimmedDesc.length() < 10) {
                errors.put("description", "Complaint description must be at least 10 characters long");
            } else if (trimmedDesc.length() > 5000) {
                errors.put("description", "Complaint description cannot exceed 5000 characters");
            } else if (containsXssOrHtml(trimmedDesc)) {
                errors.put("description",
                        "Complaint description contains forbidden HTML tags, script tags, or dangerous code");
            }
        }

        // Optional Email Validation
        if (email != null && !email.isBlank()) {
            String trimmedEmail = email.trim();
            if (trimmedEmail.length() > 100 || !trimmedEmail.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
                    || containsXssOrHtml(trimmedEmail)) {
                errors.put("email", "Invalid email format");
            }
        }

        if (!errors.isEmpty()) {
            log.warn("SECURITY REJECTION: Input validation failed: {}", sanitizeForLog(errors.toString()));
            throw new InputValidationException(errors);
        }
    }

    /**
     * Public customer registration must include an explicit consent flag.
     * Staff intake does not use this check.
     */
    public void requirePublicConsent(Object consent) {
        boolean accepted = Boolean.TRUE.equals(consent)
                || (consent instanceof String text && "true".equalsIgnoreCase(text.trim()));
        if (!accepted) {
            throw new InputValidationException(
                    Map.of("consent", "You must agree before submitting your complaint."));
        }
    }

    /**
     * Validates Ticket ID inputs to prevent injection & format manipulation.
     */
    public void validateTicketId(String ticketId) {
        if (ticketId == null || ticketId.isBlank()) {
            throw new InputValidationException(Map.of("ticketId", "Complaint ticket ID is required"));
        }
        String trimmed = ticketId.trim();
        if (!TICKET_ID_PATTERN.matcher(trimmed).matches() || containsXssOrHtml(trimmed)) {
            throw new InputValidationException(Map.of("ticketId", "Invalid complaint ticket format"));
        }
    }

    /**
     * Validates Task ID inputs.
     */
    public void validateTaskId(String taskId) {
        if (taskId == null || taskId.isBlank()) {
            throw new InputValidationException(Map.of("taskId", "Task ID is required"));
        }
        String trimmed = taskId.trim();
        if (!TASK_ID_PATTERN.matcher(trimmed).matches() || containsXssOrHtml(trimmed)) {
            throw new InputValidationException(Map.of("taskId", "Invalid task ID format"));
        }
    }

    /**
     * Validates Account Number inputs.
     */
    public void validateAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new InputValidationException(Map.of("accountNumber", "Account number is required"));
        }
        String trimmed = accountNumber.trim();
        if (!ACCOUNT_NUMBER_PATTERN.matcher(trimmed).matches() || containsXssOrHtml(trimmed)) {
            throw new InputValidationException(
                    Map.of("accountNumber", "Account number must be exactly 13 numeric digits"));
        }
    }

    /**
     * Inspects a generic Map payload for XSS/HTML injection across all internal
     * endpoint parameters.
     */
    public void validateGenericPayload(Map<String, Object> payload) {
        if (payload == null)
            return;
        Map<String, String> errors = new LinkedHashMap<>();
        validateMapRecursive(payload, "", errors);
        if (!errors.isEmpty()) {
            log.warn("SECURITY REJECTION: Malicious elements detected in request payload: {}",
                    sanitizeForLog(errors.toString()));
            throw new InputValidationException(errors);
        }
    }

    @SuppressWarnings("unchecked")
    private void validateMapRecursive(Map<String, Object> map, String prefix, Map<String, String> errors) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String strVal) {
                if (containsXssOrHtml(strVal)) {
                    errors.put(key, "Field contains forbidden script, HTML tags, or dangerous code");
                }
            } else if (value instanceof Map<?, ?> nestedMap) {
                validateMapRecursive((Map<String, Object>) nestedMap, key, errors);
            }
        }
    }

    /**
     * Checks if text contains raw, URL-encoded, or double-encoded XSS / HTML
     * injection vectors.
     */
    public boolean containsXssOrHtml(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }
        String decoded = decodeUrlPayload(input);
        return XSS_HTML_TAG_PATTERN.matcher(input).find()
                || ANY_HTML_TAG_PATTERN.matcher(input).find()
                || XSS_HTML_TAG_PATTERN.matcher(decoded).find()
                || ANY_HTML_TAG_PATTERN.matcher(decoded).find();
    }

    /**
     * Decodes URL-encoded and double URL-encoded payload strings for inspection.
     */
    public String decodeUrlPayload(String input) {
        if (input == null)
            return "";
        String decoded = input;
        try {
            int loops = 0;
            while (decoded.contains("%") && loops < 3) {
                String prev = decoded;
                decoded = URLDecoder.decode(decoded, StandardCharsets.UTF_8);
                if (prev.equalsIgnoreCase(decoded))
                    break;
                loops++;
            }
        } catch (Exception ignored) {
            // Ignore decoding failure
        }
        return decoded;
    }

    /**
     * Prevents Log Injection / CRLF Injection by replacing line breaks with
     * underscores.
     */
    public String sanitizeForLog(String logMessage) {
        if (logMessage == null)
            return "";
        return logMessage.replaceAll("[\\r\\n\\t]", "_");
    }

    public String sanitizeText(String input) {
        if (input == null)
            return null;
        return input.replaceAll("<[^>]*>", "").trim();
    }
}
