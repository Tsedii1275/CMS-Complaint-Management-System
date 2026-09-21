package com.dashenbank.cms.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
public class JwtUtils {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_PURPOSE = "purpose";
    public static final String PURPOSE_PASSWORD_CHANGE = "PASSWORD_CHANGE";
    public static final String HEADER_NEW_ACCESS_TOKEN = "X-New-Access-Token";
    private static final int MIN_SECRET_BYTES = 32;

    public enum TokenStatus {
        VALID,
        EXPIRED,
        INVALID
    }

    @Value("${app.jwtSecret}")
    private String jwtSecret;

    @Value("${app.jwtExpirationMs:" + SecurityPolicy.SESSION_TIMEOUT_MS + "}")
    private int jwtExpirationMs;

    @PostConstruct
    void validateSecret() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                    "app.jwtSecret / APP_JWT_SECRET is required and must not be empty");
        }
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "APP_JWT_SECRET must be at least " + MIN_SECRET_BYTES + " bytes for HS256");
        }
    }

    public String generateJwtToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        String role = userPrincipal.getAuthorities().iterator().next().getAuthority();
        return generateJwtToken(userPrincipal.getUsername(), role);
    }

    public String generateJwtToken(UserDetails userDetails) {
        String role = userDetails.getAuthorities().iterator().next().getAuthority();
        return generateJwtToken(userDetails.getUsername(), role);
    }

    public String generateJwtToken(String username, String role) {
        return sign(Jwts.builder()
                .subject(username)
                .claim(CLAIM_ROLE, role));
    }

    public String generatePasswordChangeToken(String username) {
        return sign(Jwts.builder()
                .subject(username)
                .claim(CLAIM_PURPOSE, PURPOSE_PASSWORD_CHANGE));
    }

    private String sign(JwtBuilder builder) {
        Instant issuedAt = Instant.now();
        return JwtValidity.apply(builder, issuedAt, jwtExpirationMs)
                .signWith(getSigningKey())
                .compact();
    }

    public boolean isPasswordChangeToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return PURPOSE_PASSWORD_CHANGE.equals(claims.get(CLAIM_PURPOSE, String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public TokenStatus inspectToken(String authToken) {
        try {
            parseClaims(authToken);
            return TokenStatus.VALID;
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
            return TokenStatus.EXPIRED;
        } catch (SecurityException e) {
            logger.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (JwtException | IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }
        return TokenStatus.INVALID;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String getSubjectLenient(String token) {
        try {
            return parseClaims(token).getSubject();
        } catch (ExpiredJwtException e) {
            return e.getClaims() != null ? e.getClaims().getSubject() : null;
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public String getUserNameFromJwtToken(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean validateJwtToken(String authToken) {
        return inspectToken(authToken) == TokenStatus.VALID;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
