package com.dashenbank.cms.controller;

import com.dashenbank.cms.dto.PasswordChangeRequest;
import com.dashenbank.cms.model.SecurityAuditEvent;
import com.dashenbank.cms.model.User;
import com.dashenbank.cms.repository.UserRepository;
import com.dashenbank.cms.security.ClientIp;
import com.dashenbank.cms.security.JwtUtils;
import com.dashenbank.cms.security.SecurityPolicy;
import com.dashenbank.cms.service.AccountLockoutService;
import com.dashenbank.cms.service.PasswordChangeService;
import com.dashenbank.cms.service.SecurityAuditService;
import jakarta.servlet.http.HttpServletRequest;
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
    private static final String KEY_PASSWORD = "password";

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final PasswordChangeService passwordChangeService;
    private final AccountLockoutService accountLockoutService;
    private final SecurityAuditService securityAuditService;

    public AuthController(AuthenticationManager authenticationManager,
            JwtUtils jwtUtils,
            UserRepository userRepository,
            PasswordChangeService passwordChangeService,
            AccountLockoutService accountLockoutService,
            SecurityAuditService securityAuditService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
        this.passwordChangeService = passwordChangeService;
        this.accountLockoutService = accountLockoutService;
        this.securityAuditService = securityAuditService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody Map<String, String> loginRequest,
            HttpServletRequest httpRequest) {
        String rawUsername = loginRequest.get(KEY_USERNAME);
        String rawPassword = loginRequest.get(KEY_PASSWORD);
        String username = rawUsername != null ? rawUsername.trim() : "";
        String password = rawPassword != null ? rawPassword.trim() : "";
        String ip = ClientIp.from(httpRequest);

        var userOpt = userRepository.findByUsernameIgnoreCase(username);
        if (userOpt.isPresent()) {
            accountLockoutService.unlockIfElapsed(userOpt.get(), ip);
            if (accountLockoutService.isLocked(userOpt.get())) {
                return lockedResponse();
            }
        }

        String targetUsername = userOpt.map(User::getUsername).orElse(username);
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(targetUsername, password));

            User user = userRepository.findByUsernameIgnoreCase(targetUsername).orElse(null);
            accountLockoutService.recordSuccessfulLogin(user);

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
            if (userOpt.isPresent() && accountLockoutService.isLocked(userOpt.get())) {
                return lockedResponse();
            }
            log.warn("Authentication failed for user: {} (password length={})",
                    loginRequest.get(KEY_USERNAME),
                    loginRequest.get(KEY_PASSWORD) == null ? -1 : loginRequest.get(KEY_PASSWORD).length());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(KEY_ERROR, "Invalid username or password", KEY_CODE, "INVALID_CREDENTIALS"));
        } catch (Exception e) {
            log.warn("Authentication failed for user: {} (password length={})",
                    loginRequest.get(KEY_USERNAME),
                    loginRequest.get(KEY_PASSWORD) == null ? -1 : loginRequest.get(KEY_PASSWORD).length(),
                    e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(KEY_ERROR, "Invalid username or password", KEY_CODE, "INVALID_CREDENTIALS"));
        }
    }

    @PutMapping("/password")
    public ResponseEntity<Map<String, String>> updatePassword(
            @RequestBody(required = false) PasswordChangeRequest request) {
        return mapPasswordResult(passwordChangeService.changePassword(request));
    }

    @PostMapping("/expired-password")
    public ResponseEntity<Map<String, String>> updateExpiredPassword(@RequestBody Map<String, String> payload) {
        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setCurrentPassword(payload.get("currentPassword"));
        request.setNewPassword(payload.get("newPassword"));
        request.setConfirmPassword(payload.get("confirmPassword"));
        return mapPasswordResult(passwordChangeService.changeExpiredPassword(
                payload.get("passwordChangeToken"), request));
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
            case UNAUTHENTICATED -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(KEY_MESSAGE, "Authentication is required."));
        };
    }

    private static ResponseEntity<Map<String, String>> lockedResponse() {
        return ResponseEntity.status(HttpStatus.LOCKED).body(Map.of(
                KEY_ERROR, SecurityPolicy.ACCOUNT_LOCKED_MESSAGE,
                KEY_MESSAGE, SecurityPolicy.ACCOUNT_LOCKED_MESSAGE,
                KEY_CODE, CODE_ACCOUNT_LOCKED));
    }
}
