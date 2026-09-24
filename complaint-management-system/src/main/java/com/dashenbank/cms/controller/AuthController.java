package com.dashenbank.cms.controller;

import com.dashenbank.cms.dto.ExpiredPasswordChangeRequest;
import com.dashenbank.cms.dto.LoginRequest;
import com.dashenbank.cms.dto.PasswordChangeRequest;
import com.dashenbank.cms.model.AuthSource;
import com.dashenbank.cms.model.SecurityAuditEvent;
import com.dashenbank.cms.model.User;
import com.dashenbank.cms.repository.UserRepository;
import com.dashenbank.cms.security.ApiSessionRegistry;
import com.dashenbank.cms.security.ClientIp;
import com.dashenbank.cms.security.JwtUtils;
import com.dashenbank.cms.security.SecurityPolicy;
import com.dashenbank.cms.security.ldap.ActiveDirectoryAuthService;
import com.dashenbank.cms.security.ldap.DirectoryAccountDisabledException;
import com.dashenbank.cms.security.ldap.DirectoryAuthenticationException;
import com.dashenbank.cms.security.ldap.DirectoryUnavailableException;
import com.dashenbank.cms.security.ldap.LdapProperties;
import com.dashenbank.cms.security.ldap.RoleNotMappedException;
import com.dashenbank.cms.service.AccountLockoutService;
import com.dashenbank.cms.service.PasswordChangeService;
import com.dashenbank.cms.service.SecurityAuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final String KEY_USERNAME = "username";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_ERROR = "error";
    private static final String KEY_CODE = "code";
    private static final String CODE_ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
    private static final String CODE_INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    private static final String MSG_INVALID_CREDENTIALS = "Invalid username or password";
    private static final String LOG_AUTH_FAILED = "Authentication failed for user: {}";
    private static final String CODE_ROLE_NOT_MAPPED = "ROLE_NOT_MAPPED";
    private static final String CODE_LDAP_UNAVAILABLE = "LDAP_UNAVAILABLE";
    private static final String CODE_ACCOUNT_DISABLED = "ACCOUNT_DISABLED";

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final PasswordChangeService passwordChangeService;
    private final AccountLockoutService accountLockoutService;
    private final SecurityAuditService securityAuditService;
    private final LdapProperties ldapProperties;
    private final ActiveDirectoryAuthService activeDirectoryAuthService;
    private final ApiSessionRegistry apiSessionRegistry;

    public AuthController(AuthenticationManager authenticationManager,
            JwtUtils jwtUtils,
            UserRepository userRepository,
            PasswordChangeService passwordChangeService,
            AccountLockoutService accountLockoutService,
            SecurityAuditService securityAuditService,
            LdapProperties ldapProperties,
            ActiveDirectoryAuthService activeDirectoryAuthService,
            ApiSessionRegistry apiSessionRegistry) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
        this.passwordChangeService = passwordChangeService;
        this.accountLockoutService = accountLockoutService;
        this.securityAuditService = securityAuditService;
        this.ldapProperties = ldapProperties;
        this.activeDirectoryAuthService = activeDirectoryAuthService;
        this.apiSessionRegistry = apiSessionRegistry;
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest httpRequest) {
        String rawUsername = loginRequest.getUsername();
        String rawPassword = loginRequest.getPassword();
        String username = rawUsername != null ? rawUsername.trim() : "";
        String password = rawPassword != null ? rawPassword.trim() : "";
        String ip = ClientIp.from(httpRequest);

        var userOpt = userRepository.findByUsernameIgnoreCase(username);
        if (userOpt.isPresent() && userOpt.get().getAuthSource() != AuthSource.AD) {
            accountLockoutService.unlockIfElapsed(userOpt.get(), ip);
            if (accountLockoutService.isLocked(userOpt.get())) {
                return lockedResponse();
            }
        }

        if (useDirectory(userOpt.orElse(null))) {
            return authenticateWithDirectory(username, password, httpRequest);
        }

        String targetUsername = userOpt.map(User::getUsername).orElse(username);
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(targetUsername, password));

            User user = userRepository.findByUsernameIgnoreCase(targetUsername).orElse(null);
            accountLockoutService.recordSuccessfulLogin(user);
            securityAuditService.log(userDetailsName(authentication), SecurityAuditEvent.SUCCESSFUL_LOGIN, ip);
            apiSessionRegistry.opened(userDetailsName(authentication), roleOf(authentication), ip,
                    httpRequest.getHeader("User-Agent"));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String jwt = jwtUtils.generateJwtToken(authentication);

            Map<String, Object> response = new HashMap<>();
            response.put("token", jwt);
            response.put(KEY_USERNAME, userDetails.getUsername());
            response.put("role", userDetails.getAuthorities().iterator().next().getAuthority());
            if (user != null) {
                response.put("district", user.getDistrict());
                response.put("branch", user.getBranch());
                response.put("department", user.getDepartment());
                response.put("fullName", user.getFullName());
                response.put("mustChangePassword", user.isMustChangePassword());
            }
            return ResponseEntity.ok(response);
        } catch (LockedException e) {
            return lockedResponse();
        } catch (CredentialsExpiredException e) {
            userOpt.ifPresent(accountLockoutService::recordSuccessfulLogin);
            String expiredUser = userOpt.map(User::getUsername).orElse(targetUsername);
            securityAuditService.log(expiredUser, SecurityAuditEvent.PASSWORD_EXPIRED, ip);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    KEY_ERROR, SecurityPolicy.PASSWORD_EXPIRED_MESSAGE,
                    KEY_MESSAGE, SecurityPolicy.PASSWORD_EXPIRED_MESSAGE,
                    KEY_CODE, "PASSWORD_EXPIRED",
                    KEY_USERNAME, expiredUser,
                    "passwordChangeToken", jwtUtils.generatePasswordChangeToken(expiredUser)));
        } catch (BadCredentialsException e) {
            userOpt.ifPresent(user -> accountLockoutService.recordFailedAttempt(user, ip));
            if (userOpt.isEmpty()) {
                securityAuditService.log(username, SecurityAuditEvent.FAILED_LOGIN, ip);
            }
            if (userOpt.isPresent() && accountLockoutService.isLocked(userOpt.get())) {
                return lockedResponse();
            }
            log.warn(LOG_AUTH_FAILED, username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(KEY_ERROR, MSG_INVALID_CREDENTIALS, KEY_CODE, CODE_INVALID_CREDENTIALS));
        } catch (Exception e) {
            log.warn(LOG_AUTH_FAILED, username, e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(KEY_ERROR, MSG_INVALID_CREDENTIALS, KEY_CODE, CODE_INVALID_CREDENTIALS));
        }
    }

    private ResponseEntity<?> authenticateWithDirectory(String username, String password,
            HttpServletRequest httpRequest) {
        String ip = ClientIp.from(httpRequest);
        try {
            User user = activeDirectoryAuthService.login(username, password);
            if (!user.isEnabled()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        KEY_ERROR, "This account is disabled.",
                        KEY_MESSAGE, "This account is disabled.",
                        KEY_CODE, CODE_ACCOUNT_DISABLED));
            }
            accountLockoutService.recordSuccessfulLogin(user);
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    org.springframework.security.core.userdetails.User.builder()
                            .username(user.getUsername())
                            .password(user.getPassword())
                            .disabled(!user.isEnabled())
                            .accountExpired(false)
                            .credentialsExpired(false)
                            .accountLocked(false)
                            .authorities(user.getRole().name())
                            .build(),
                    null,
                    java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                            user.getRole().name())));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            securityAuditService.log(user.getUsername(), SecurityAuditEvent.SUCCESSFUL_LOGIN, ip);
            apiSessionRegistry.opened(user.getUsername(), user.getRole().name(), ip,
                    httpRequest.getHeader("User-Agent"));
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String jwt = jwtUtils.generateJwtToken(authentication);
            Map<String, Object> response = new HashMap<>();
            response.put("token", jwt);
            response.put(KEY_USERNAME, userDetails.getUsername());
            response.put("role", userDetails.getAuthorities().iterator().next().getAuthority());
            response.put("district", user.getDistrict());
            response.put("branch", user.getBranch());
            response.put("department", user.getDepartment());
            response.put("fullName", user.getFullName());
            response.put("mustChangePassword", false);
            return ResponseEntity.ok(response);
        } catch (RoleNotMappedException e) {
            log.warn("AD login rejected: no CMS role mapping for user {}", username);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    KEY_ERROR, e.getMessage(),
                    KEY_MESSAGE, e.getMessage(),
                    KEY_CODE, CODE_ROLE_NOT_MAPPED));
        } catch (DirectoryAccountDisabledException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    KEY_ERROR, e.getMessage(),
                    KEY_MESSAGE, e.getMessage(),
                    KEY_CODE, CODE_ACCOUNT_DISABLED));
        } catch (DirectoryUnavailableException e) {
            log.warn("LDAP unavailable during login");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    KEY_ERROR, "Active Directory is unavailable. Local accounts can still sign in.",
                    KEY_MESSAGE, "Active Directory is unavailable. Local accounts can still sign in.",
                    KEY_CODE, CODE_LDAP_UNAVAILABLE));
        } catch (DirectoryAuthenticationException e) {
            securityAuditService.log(username, SecurityAuditEvent.FAILED_LOGIN, ip);
            log.warn(LOG_AUTH_FAILED, username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(KEY_ERROR, MSG_INVALID_CREDENTIALS, KEY_CODE, CODE_INVALID_CREDENTIALS));
        }
    }

    private boolean useDirectory(User existing) {
        if (!ldapProperties.isEnabled()) {
            return false;
        }
        if (existing == null) {
            return true;
        }
        return existing.getAuthSource() == AuthSource.AD;
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(Authentication authentication, HttpServletRequest httpRequest) {
        String username = authentication == null ? "unknown" : authentication.getName();
        securityAuditService.log(username, SecurityAuditEvent.LOGOUT, ClientIp.from(httpRequest));
        apiSessionRegistry.closed(username);
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of(KEY_MESSAGE, "Logged out"));
    }

    @PutMapping("/password")
    public ResponseEntity<Map<String, String>> updatePassword(
            @Valid @RequestBody PasswordChangeRequest request) {
        return mapPasswordResult(passwordChangeService.changePassword(request));
    }

    @PostMapping("/expired-password")
    public ResponseEntity<Map<String, String>> updateExpiredPassword(
            @Valid @RequestBody ExpiredPasswordChangeRequest payload) {
        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setCurrentPassword(payload.getCurrentPassword());
        request.setNewPassword(payload.getNewPassword());
        request.setConfirmPassword(payload.getConfirmPassword());
        return mapPasswordResult(passwordChangeService.changeExpiredPassword(
                payload.getPasswordChangeToken(), request));
    }

    private ResponseEntity<Map<String, String>> mapPasswordResult(PasswordChangeService.Result result) {
        return switch (result) {
            case SUCCESS -> ResponseEntity.ok(Map.of(KEY_MESSAGE, "Password updated successfully."));
            case CURRENT_INCORRECT -> ResponseEntity.badRequest()
                    .body(Map.of(KEY_MESSAGE, "Current password is incorrect."));
            case CONFIRM_MISMATCH -> ResponseEntity.badRequest()
                    .body(Map.of(KEY_MESSAGE, "New password and confirmation do not match."));
            case INVALID_NEW -> ResponseEntity.badRequest()
                    .body(Map.of(KEY_MESSAGE, SecurityPolicy.PASSWORD_REQUIREMENTS_MESSAGE));
            case HISTORY_REUSE -> ResponseEntity.badRequest()
                    .body(Map.of(KEY_MESSAGE, SecurityPolicy.PASSWORD_HISTORY_MESSAGE));
            case DIRECTORY_MANAGED -> ResponseEntity.badRequest()
                    .body(Map.of(KEY_MESSAGE, "This account uses Active Directory. Change the password in AD."));
            case UNAUTHENTICATED -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(KEY_MESSAGE, "Authentication is required."));
        };
    }

    private static String userDetailsName(Authentication authentication) {
        return authentication.getName();
    }

    private static String roleOf(Authentication authentication) {
        if (authentication.getAuthorities() == null || authentication.getAuthorities().isEmpty()) {
            return "";
        }
        return authentication.getAuthorities().iterator().next().getAuthority();
    }

    private static ResponseEntity<Map<String, String>> lockedResponse() {
        return ResponseEntity.status(HttpStatus.LOCKED).body(Map.of(
                KEY_ERROR, SecurityPolicy.ACCOUNT_LOCKED_MESSAGE,
                KEY_MESSAGE, SecurityPolicy.ACCOUNT_LOCKED_MESSAGE,
                KEY_CODE, CODE_ACCOUNT_LOCKED));
    }
}
