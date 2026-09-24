package com.dashenbank.cms.security;

import java.util.List;

/**
 * Published RBAC map for the security dashboard. Matches
 * {@link WebSecurityConfig} matcher groups.
 */
public final class SecurityRbacCatalog {

    private SecurityRbacCatalog() {
    }

    public static List<RbacRow> rows() {
        return List.of(
                row("ROLE_ADMIN", "Admin APIs",
                        "/api/admin/**, /api/users/**, /api/sla/config/**, /api/rca/**, /api/nbe-compliance-reports/**, /api/audit/logs"),
                row("ROLE_CUSTOMER_CARE_OFFICER", "Operational",
                        "/api/tasks/**, /api/process/**, /api/complaints/staff-submit, /api/customer-profile/**, /api/users/officers"),
                row("ROLE_CUSTOMER_CARE_TEAM_LEADER", "Operational + officers",
                        "/api/tasks/**, /api/process/**, /api/users/officers, /api/customer-profile/**"),
                row("ROLE_CUSTOMER_CARE_SENIOR_MANAGER", "Operational + officers",
                        "/api/tasks/**, /api/process/**, /api/users/officers, /api/sla/alerts/**"),
                row("ROLE_SERVICE_QUALITY_DIRECTOR", "Operational + officers",
                        "/api/tasks/**, /api/process/**, /api/users/officers, /api/cmd/analytics/**"),
                row("ROLE_AUDIT_INVESTIGATION_TEAM", "Audit workspace",
                        "/api/audit/sla/**, /api/tasks/** (authenticated)"),
                row("PUBLIC", "Unauthenticated intake",
                        "POST /api/auth/login, POST /api/complaints/start, GET /api/complaints/status/**, /api/hierarchy"),
                row("AUTHENTICATED", "Any signed-in staff",
                        "/api/tasks/**, /api/process/**, /api/customer-profile/**, PUT /api/auth/password"));
    }

    public static List<ValidationRule> validationRules() {
        return List.of(
                new ValidationRule("LoginRequest", "username, password", "@NotBlank @Size"),
                new ValidationRule("UserCreateRequest", "username, password, email", "@NotBlank @Size @Email"),
                new ValidationRule("PasswordChangeRequest", "current/new/confirm", "@NotBlank @Size(min=12)"),
                new ValidationRule("Complaint start", "name, phone, channel, description",
                        "Controller checks + Ethiopian phone pattern; 400 on failure"),
                new ValidationRule("Customer profile lookup", "accountNumber", "13-digit pattern; 400 INVALID_ACCOUNT_NUMBER"),
                new ValidationRule("Ticket / status lookup", "ticket id path", "400/404 structured error body"),
                new ValidationRule("Task complete", "taskId + payload", "Authenticated; 400 INVALID_REQUEST_PAYLOAD"));
    }

    public static List<HttpHeaderSetting> httpHeaders() {
        return List.of(
                new HttpHeaderSetting("Strict-Transport-Security", "max-age=31536000; includeSubDomains"),
                new HttpHeaderSetting("X-Content-Type-Options", "nosniff"),
                new HttpHeaderSetting("X-Frame-Options", "DENY"),
                new HttpHeaderSetting("Referrer-Policy", "strict-origin-when-cross-origin"),
                new HttpHeaderSetting("Content-Security-Policy",
                        "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'"),
                new HttpHeaderSetting("Permissions-Policy", "camera=(), microphone=(), geolocation=()"));
    }

    public static List<RateLimitRule> rateLimits() {
        return List.of(
                new RateLimitRule("POST /api/auth/login", 10, 60),
                new RateLimitRule("POST /api/complaints/start", 20, 60),
                new RateLimitRule("GET /api/complaints/status/**", 30, 60),
                new RateLimitRule("GET /api/customer-profile/**", 30, 60),
                new RateLimitRule("POST /api/complaints/upload-evidence", 10, 60));
    }

    private static RbacRow row(String role, String permissions, String apis) {
        return new RbacRow(role, apis, permissions);
    }

    public record RbacRow(String role, String assignedApis, String permissions) {
    }

    public record ValidationRule(String surface, String fields, String constraints) {
    }

    public record HttpHeaderSetting(String name, String value) {
    }

    public record RateLimitRule(String endpoint, int limit, int windowSeconds) {
    }
}
