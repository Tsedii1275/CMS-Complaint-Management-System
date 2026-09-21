package com.dashenbank.cms.security;

import io.jsonwebtoken.JwtBuilder;

import java.time.Instant;
import java.util.Date;

/**
 * JJWT 0.13 {@code issuedAt}/{@code expiration} still take {@link Date}.
 */
@SuppressWarnings("java:S2143")
final class JwtValidity {

    private JwtValidity() {
    }

    @SuppressWarnings("java:S2143")
    static JwtBuilder apply(JwtBuilder builder, Instant issuedAt, long expirationMs) {
        return builder
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plusMillis(expirationMs)));
    }
}
