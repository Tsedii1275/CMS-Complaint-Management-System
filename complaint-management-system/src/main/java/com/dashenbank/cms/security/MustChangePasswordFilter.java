package com.dashenbank.cms.security;

import com.dashenbank.cms.model.User;
import com.dashenbank.cms.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class MustChangePasswordFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    public MustChangePasswordFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String path = request.getServletPath();
        return "/api/auth/login".equals(path)
                || "/api/auth/expired-password".equals(path)
                || ("PUT".equalsIgnoreCase(request.getMethod()) && "/api/auth/password".equals(path))
                || "/api/users/password-status".equals(path)
                || "/favicon.ico".equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null
                && !"anonymousUser".equalsIgnoreCase(auth.getName())) {
            User user = userRepository.findByUsernameIgnoreCase(auth.getName()).orElse(null);
            if (user != null && user.isMustChangePassword()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write(
                        "{\"error\":\"Password change is required before continuing.\",\"code\":\"PASSWORD_CHANGE_REQUIRED\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
