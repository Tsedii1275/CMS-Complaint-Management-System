package com.dashenbank.cms.security;

import com.dashenbank.cms.model.SecurityAuditEvent;
import com.dashenbank.cms.service.SecurityAuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class AuthEntryPointJwt implements AuthenticationEntryPoint {
    private static final Logger logger = LoggerFactory.getLogger(AuthEntryPointJwt.class);

    private final SecurityAuditService securityAuditService;
    private final ObjectMapper objectMapper;

    public AuthEntryPointJwt(SecurityAuditService securityAuditService, ObjectMapper objectMapper) {
        this.securityAuditService = securityAuditService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        boolean deactivated = Boolean.TRUE.equals(request.getAttribute(AuthTokenFilter.ATTR_ACCOUNT_DISABLED));
        boolean expired = Boolean.TRUE.equals(request.getAttribute(AuthTokenFilter.ATTR_JWT_EXPIRED));
        logger.error("Unauthorized error on path {}: {}", request.getRequestURI(), authException.getMessage());

        String code = deactivated ? "ACCOUNT_DISABLED" : (expired ? "SESSION_TIMEOUT" : "UNAUTHORIZED");
        String message = deactivated ? SecurityPolicy.ACCOUNT_DISABLED_MESSAGE
                : (expired ? SecurityPolicy.SESSION_TIMEOUT_MESSAGE : "Error: Unauthorized");
        if (deactivated || expired) {
            Object username = request.getAttribute(AuthTokenFilter.ATTR_JWT_USERNAME);
            securityAuditService.log(username != null ? username.toString() : "unknown",
                    deactivated ? SecurityAuditEvent.ACCOUNT_DISABLED : SecurityAuditEvent.SESSION_TIMEOUT,
                    ClientIp.from(request));
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Map.of(
                "error", message,
                "code", code,
                "message", message));
    }
}
