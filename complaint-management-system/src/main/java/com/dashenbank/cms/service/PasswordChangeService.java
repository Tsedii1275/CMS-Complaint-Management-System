package com.dashenbank.cms.service;

import com.dashenbank.cms.dto.PasswordChangeRequest;
import com.dashenbank.cms.model.User;
import com.dashenbank.cms.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
public class PasswordChangeService {

    public enum Result {
        SUCCESS,
        UNAUTHENTICATED,
        CURRENT_INCORRECT,
        CONFIRM_MISMATCH,
        INVALID_NEW
    }

    private static final Logger log = LoggerFactory.getLogger(PasswordChangeService.class);
    private static final Pattern PASSWORD_POLICY = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordChangeService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Result changePassword(PasswordChangeRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null
                || "anonymousUser".equalsIgnoreCase(authentication.getName())) {
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
        if (isBlank(currentPassword)) {
            return Result.CURRENT_INCORRECT;
        }

        User user = userRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        if (user == null) {
            log.warn("Password change requested for missing authenticated principal");
            return Result.UNAUTHENTICATED;
        }

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return Result.CURRENT_INCORRECT;
        }
        if (currentPassword.equals(newPassword) || !meetsPolicy(newPassword)) {
            return Result.INVALID_NEW;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);
        log.info("Password updated for authenticated user {}", user.getUsername());
        return Result.SUCCESS;
    }

    private static boolean meetsPolicy(String password) {
        return password != null && PASSWORD_POLICY.matcher(password).matches();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
