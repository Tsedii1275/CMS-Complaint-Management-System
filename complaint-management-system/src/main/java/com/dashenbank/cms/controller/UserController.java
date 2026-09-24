package com.dashenbank.cms.controller;

import com.dashenbank.cms.dto.PasswordResetRequest;
import com.dashenbank.cms.dto.UserCreateRequest;
import com.dashenbank.cms.model.Role;
import com.dashenbank.cms.model.SecurityAuditEvent;
import com.dashenbank.cms.model.User;
import com.dashenbank.cms.repository.UserRepository;
import com.dashenbank.cms.security.ClientIp;
import com.dashenbank.cms.security.SecurityPolicy;
import com.dashenbank.cms.service.PasswordPolicyService;
import com.dashenbank.cms.service.SecurityAuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping({"/api/admin/users", "/api/users"})
public class UserController {

    private static final String KEY_ERROR = "error";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_FULL_NAME = "fullName";
    private static final String KEY_DISTRICT = "district";
    private static final String KEY_BRANCH = "branch";
    private static final String KEY_DEPARTMENT = "department";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_MESSAGE = "message";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;
    private final SecurityAuditService securityAuditService;

    @Autowired
    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder,
            PasswordPolicyService passwordPolicyService, SecurityAuditService securityAuditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyService = passwordPolicyService;
        this.securityAuditService = securityAuditService;
    }

    @GetMapping("/password-status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> passwordStatus() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }
        User user = userRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(passwordPolicyService.passwordStatus(user));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userRepository.findAll();
        // Hide hashed password in API responses
        users.forEach(u -> u.setPassword(null));
        return ResponseEntity.ok(users);
    }

    @GetMapping("/officers")
    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER_CARE_OFFICER', 'ROLE_CUSTOMER_CARE_TEAM_LEADER', 'ROLE_CUSTOMER_CARE_SENIOR_MANAGER', 'ROLE_SERVICE_QUALITY_DIRECTOR', 'ROLE_ADMIN')")
    public ResponseEntity<List<User>> getCustomerCareOfficers() {
        List<User> officers = userRepository.findAll().stream()
                .filter(u -> u.isEnabled() && (u.getRole() == Role.ROLE_CUSTOMER_CARE_OFFICER
                        || u.getRole() == Role.ROLE_CUSTOMER_CARE_TEAM_LEADER))
                .toList();
        officers.forEach(u -> u.setPassword(null));
        return ResponseEntity.ok(officers);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Object> createUser(@Valid @RequestBody UserCreateRequest payload) {
        String username = payload.getUsername();
        String email = payload.getEmail();
        String password = payload.getPassword();
        String fullName = payload.getFullName();
        String roleStr = payload.getRole();
        String district = payload.getDistrict();
        String branch = payload.getBranch();
        String department = payload.getDepartment();

        PasswordPolicyService.Validation validation = passwordPolicyService.validateNewPassword(null, password);
        if (validation != PasswordPolicyService.Validation.VALID) {
            return ResponseEntity.badRequest()
                    .body(Map.of(KEY_ERROR, SecurityPolicy.PASSWORD_REQUIREMENTS_MESSAGE));
        }

        if (userRepository.findByUsernameIgnoreCase(username).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of(KEY_ERROR, "Username already exists."));
        }

        Role role = Role.ROLE_DEPARTMENT_WORKUNIT;
        if (roleStr != null && !roleStr.isBlank()) {
            try {
                role = Role.valueOf(roleStr);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of(KEY_ERROR, "Invalid role specified: " + roleStr));
            }
        }

        User newUser = User.builder()
                .username(username.trim())
                .email(email != null ? email.trim() : username.trim() + "@dashenbank.com")
                .password(passwordEncoder.encode(password))
                .fullName(fullName != null ? fullName.trim() : username)
                .role(role)
                .authSource(com.dashenbank.cms.model.AuthSource.LOCAL)
                .district(district)
                .branch(branch)
                .department(department)
                .enabled(true)
                .mustChangePassword(true)
                .build();
        passwordPolicyService.stampInitialPasswordMetadata(newUser);

        User saved = userRepository.save(newUser);
        saved.setPassword(null);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Object> updateUser(@PathVariable Long id, @RequestBody Map<String, String> payload,
            HttpServletRequest request) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        if (payload.containsKey(KEY_FULL_NAME)) user.setFullName(payload.get(KEY_FULL_NAME));
        if (payload.containsKey(KEY_EMAIL)) user.setEmail(payload.get(KEY_EMAIL));
        if (payload.containsKey(KEY_DISTRICT)) user.setDistrict(payload.get(KEY_DISTRICT));
        if (payload.containsKey(KEY_BRANCH)) user.setBranch(payload.get(KEY_BRANCH));
        if (payload.containsKey(KEY_DEPARTMENT)) user.setDepartment(payload.get(KEY_DEPARTMENT));

        if (payload.containsKey("role") && payload.get("role") != null) {
            try {
                Role previous = user.getRole();
                Role next = Role.valueOf(payload.get("role"));
                user.setRole(next);
                if (previous != next) {
                    securityAuditService.log(user.getUsername(), SecurityAuditEvent.ROLE_CHANGED,
                            ClientIp.from(request));
                }
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of(KEY_ERROR, "Invalid role: " + payload.get("role")));
            }
        }

        if (payload.containsKey(KEY_ENABLED)) {
            user.setEnabled(Boolean.parseBoolean(payload.get(KEY_ENABLED)));
        }

        User updated = userRepository.save(user);
        updated.setPassword(null);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{id}/toggle-status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Object> toggleUserStatus(@PathVariable Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        user.setEnabled(!user.isEnabled());
        User updated = userRepository.save(user);
        updated.setPassword(null);
        return ResponseEntity.ok(Map.of(
            "id", updated.getId(),
            KEY_ENABLED, updated.isEnabled(),
            KEY_MESSAGE, "User " + updated.getUsername() + " status changed to " + (updated.isEnabled() ? "Active" : "Inactive")
        ));
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Object> resetPassword(@PathVariable Long id, @Valid @RequestBody PasswordResetRequest payload,
            HttpServletRequest request) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String newPassword = payload.getNewPassword();

        User user = userOpt.get();
        if (user.getAuthSource() == com.dashenbank.cms.model.AuthSource.AD) {
            return ResponseEntity.badRequest().body(Map.of(KEY_ERROR,
                    "This account uses Active Directory. Reset the password in AD."));
        }

        PasswordPolicyService.Validation validation = passwordPolicyService.validateNewPassword(user, newPassword);
        if (validation == PasswordPolicyService.Validation.INVALID_POLICY) {
            return ResponseEntity.badRequest()
                    .body(Map.of(KEY_ERROR, SecurityPolicy.PASSWORD_REQUIREMENTS_MESSAGE));
        }
        if (validation == PasswordPolicyService.Validation.HISTORY_REUSE) {
            return ResponseEntity.badRequest()
                    .body(Map.of(KEY_ERROR, SecurityPolicy.PASSWORD_HISTORY_MESSAGE));
        }

        passwordPolicyService.assignPassword(user, newPassword);
        user.setMustChangePassword(true);
        userRepository.save(user);
        securityAuditService.log(user.getUsername(), SecurityAuditEvent.PASSWORD_RESET, ClientIp.from(request));

        return ResponseEntity.ok(Map.of(KEY_MESSAGE, "Password for user " + user.getUsername() + " successfully reset."));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Object> deleteUser(@PathVariable Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        userRepository.delete(user);
        return ResponseEntity.ok(Map.of(KEY_MESSAGE, "User " + user.getUsername() + " deleted successfully."));
    }
}
