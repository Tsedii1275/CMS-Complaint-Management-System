package com.dashenbank.cms.controller;

import com.dashenbank.cms.dto.PasswordChangeRequest;
import com.dashenbank.cms.security.JwtUtils;
import com.dashenbank.cms.service.PasswordChangeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
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

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    private com.dashenbank.cms.repository.UserRepository userRepository;

    @Autowired
    private PasswordChangeService passwordChangeService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody Map<String, String> loginRequest) {
        try {
            String rawUsername = loginRequest.get(KEY_USERNAME);
            String password = loginRequest.get("password");
            String username = rawUsername != null ? rawUsername.trim() : "";

            var userOpt = userRepository.findByUsernameIgnoreCase(username);
            String targetUsername = userOpt.isPresent() ? userOpt.get().getUsername() : username;

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(targetUsername, password));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateJwtToken(authentication);
            
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String role = userDetails.getAuthorities().iterator().next().getAuthority();

            Map<String, Object> response = new HashMap<>();
            response.put("token", jwt);
            response.put(KEY_USERNAME, userDetails.getUsername());
            response.put("role", role);

            // Fetch and append user profile attributes including full name
            var dbUserOpt = userRepository.findByUsernameIgnoreCase(userDetails.getUsername());
            if (dbUserOpt.isPresent()) {
                var u = dbUserOpt.get();
                response.put("district", u.getDistrict());
                response.put("branch", u.getBranch());
                response.put("department", u.getDepartment());
                response.put("fullName", u.getFullName());
                response.put("mustChangePassword", u.isMustChangePassword());
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.warn("Authentication failed for user: {}", loginRequest.get(KEY_USERNAME), e);
            return ResponseEntity.status(401).body(Map.of("error", "Invalid username or password"));
        }
    }

    @PutMapping("/password")
    public ResponseEntity<Map<String, String>> updatePassword(@RequestBody(required = false) PasswordChangeRequest request) {
        try {
            PasswordChangeService.Result result = passwordChangeService.changePassword(request);
            return switch (result) {
                case SUCCESS -> ResponseEntity.ok(Map.of(KEY_MESSAGE, "Password updated successfully."));
                case CURRENT_INCORRECT -> ResponseEntity.badRequest()
                        .body(Map.of(KEY_MESSAGE, "Current password is incorrect."));
                case CONFIRM_MISMATCH -> ResponseEntity.badRequest()
                        .body(Map.of(KEY_MESSAGE, "New password and confirmation do not match."));
                case INVALID_NEW -> ResponseEntity.badRequest()
                        .body(Map.of(KEY_MESSAGE, "New password does not meet the password requirements."));
                case UNAUTHENTICATED -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(KEY_MESSAGE, "Authentication is required."));
            };
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(KEY_MESSAGE, "Unable to update password. Please try again."));
        }
    }
}
