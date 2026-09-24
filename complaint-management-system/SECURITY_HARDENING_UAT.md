# UAT screenshot guide — API Security Hardening

Use an **admin** staff account. Open **Security Hardening** in the sidebar (`/admin/security`). Click **Refresh** before each capture.

| Checklist item | Where to screenshot | What auditors should see |
| --- | --- | --- |
| 1. Version / patch visibility | Card **1. System Information** | Application version, build version, build timestamp, Spring Boot version, Java runtime, environment, database version |
| 2. Authentication | Card **2. Authentication** + login events table | LDAP, local fallback, JWT, lockout threshold/duration, failed-login counter, MFA_READY=true, `MfaProvider` extension point (not enabled in login) |
| 3. Authorization / RBAC | Card **3. Authorization** | Role, assigned APIs, permissions. Also capture a non-admin `GET /api/admin/security/rbac` **403** |
| 4. Data protection | Card **4. Encryption** | BCrypt, AES-256-GCM ready, key management status, masking `0911****44` and `cust****@email.com` |
| 5–8 / 12. Secure headers | Card **8. HTTP Security Headers** + browser DevTools Network | HSTS, X-Content-Type-Options, X-Frame-Options, Referrer-Policy, CSP, Permissions-Policy (configured and observed) |
| 6. Input validation | **Validation configuration summary** + a 400 response | Structured `{ code: VALIDATION_ERROR, message, fields }` on login / user create / complaint start / ticket lookup |
| 7. Audit logs | Card **6. Audit Logs** | Event type, count, last occurrence for login, logout, failed login, complaint, claim, complete, role change, LDAP sync, notification |
| 8. Active API connections | Card **5. Active Sessions** | Username, role, login time, source IP, user agent. **No JWT** |
| 9. Token security | Card **9. Token Security** | Access TTL 15 minutes, HS256, issuer `dashenbank-cms`, expired tokens rejected |
| 10. Rate limiting | Card **7. Rate Limits** | `/api/auth/login`, `/api/complaints/start`, `/api/complaints/status` with 429 when exceeded |
| 11. Key management | Card **10. Key Management** | JWT / LDAP / SMTP **source** only (`ENVIRONMENT`, never the secret value) |

Optional API checks (same data as the page):

- `GET /api/admin/security/system-info`
- `GET /api/admin/security/auth-events`
- `GET /api/admin/security/rbac`
- `GET /api/admin/security/encryption`
- `GET /api/admin/security/http-headers`
- `GET /api/admin/security/http-security`
- `GET /api/admin/security/audit-summary`
- `GET /api/admin/security/active-sessions`
- `GET /api/admin/security/token-config`
- `GET /api/admin/security/rate-limit-status`
- `GET /api/admin/security/key-management`
- `GET /api/admin/security/dashboard`
