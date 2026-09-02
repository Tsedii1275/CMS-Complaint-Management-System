package com.dashenbank.cms.service;

import com.dashenbank.cms.dto.PasswordChangeRequest;
import com.dashenbank.cms.model.SecurityAuditEvent;
import com.dashenbank.cms.model.User;
import com.dashenbank.cms.repository.UserRepository;
import com.dashenbank.cms.security.ClientIp;
import com.dashenbank.cms.security.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class PasswordChangeService {

    public enum Result {
        SUCCESS,
        UNAUTHENTICATED,
        CURRENT_INCORRECT,
        CONFIRM_MISMATCH,
        INVALID_NEW,
        HISTORY_REUSE
    }

    private static final Logger log = LoggerFactory.getLogger(PasswordChangeService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;
    private final SecurityAuditService securityAuditService;
    private final JwtUtils jwtUtils;

    public PasswordChangeService(UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            PasswordPolicyService passwordPolicyService,
            SecurityAuditService securityAuditService,
            JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyService = passwordPolicyService;
        this.securityAuditService = securityAuditService;
        this.jwtUtils = jwtUtils;
    }

    @Transactional
    public Result changePassword(PasswordChangeRequest request) {
        return changePasswordForUser(resolveAuthenticatedUser(), request, SecurityAuditEvent.PASSWORD_CHANGED);
    }

    @Transactional
    public Result changeExpiredPassword(String passwordChangeToken, PasswordChangeRequest request) {
        if (passwordChangeToken == null || !jwtUtils.validateJwtToken(passwordChangeToken)
                || !jwtUtils.isPasswordChangeToken(passwordChangeToken)) {
            return Result.UNAUTHENTICATED;
        }
        String username = jwtUtils.getUserNameFromJwtToken(passwordChangeToken);
        User user = userRepository.findByUsernameIgnoreCase(username).orElse(null);
        return changePasswordForUser(user, request, SecurityAuditEvent.PASSWORD_CHANGED);
    }

    private Result changePasswordForUser(User user, PasswordChangeRequest request, SecurityAuditEvent event) {
        if (user == null) {
            return Result.UNAUTHENTICATED;
        }

        String currentPassword = request == null ? null : request.getCurrentPassword();
        String newPassword = request == null ? null : request.getNewPassword();
        String confirmPassword = request == null ? null : request.getConfirmPassword();

        if (isBlank(newPassword) || isBlank(confirmPassword)) {
            return Result.INVALID_NEW;
        }
        if (!newPassword.equals(confirmPassword)) {
            return Result.CONFIRM_MISMATCH;
        }
        if (isBlank(currentPassword) || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            return Result.CURRENT_INCORRECT;
        }

        PasswordPolicyService.Validation validation = passwordPolicyService.validateNewPassword(user, newPassword);
        if (validation == PasswordPolicyService.Validation.INVALID_POLICY) {
            return Result.INVALID_NEW;
        }
        if (validation == PasswordPolicyService.Validation.HISTORY_REUSE) {
            return Result.HISTORY_REUSE;
        }

        passwordPolicyService.assignPassword(user, newPassword);
        userRepository.save(user);
        securityAuditService.log(user.getUsername(), event, currentIp());
        log.info("Password updated for authenticated user {}", user.getUsername());
        return Result.SUCCESS;
    }

    private User resolveAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null
                || "anonymousUser".equalsIgnoreCase(authentication.getName())) {
            return null;
        }
        return userRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
    }

    private static String currentIp() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            HttpServletRequest request = servletAttrs.getRequest();
            return ClientIp.from(request);
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
