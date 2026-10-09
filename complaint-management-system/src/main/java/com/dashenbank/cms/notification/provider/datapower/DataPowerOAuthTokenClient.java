package com.dashenbank.cms.notification.provider.datapower;

import com.dashenbank.cms.notification.NotificationProperties;
import com.dashenbank.cms.notification.provider.ProviderResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe OAuth2 client_credentials token cache. Refresh is single-flight.
 * Access tokens are never logged.
 */
public class DataPowerOAuthTokenClient {

    private static final Logger log = LoggerFactory.getLogger(DataPowerOAuthTokenClient.class);
    private static final long DEFAULT_EXPIRES_IN_SECONDS = 3600L;
    private static final int MAX_DETAIL = 300;

    private final NotificationProperties.DataPower config;
    private final RestClient http;
    private final Clock clock;
    private final SmsMetrics metrics;
    private final String providerId;
    private final ReentrantLock lock = new ReentrantLock();
    private final ConcurrentHashMap<String, CachedToken> cachedByUrl = new ConcurrentHashMap<>();

    public DataPowerOAuthTokenClient(NotificationProperties.DataPower config, RestClient http, Clock clock,
            SmsMetrics metrics, String providerId) {
        this.config = config;
        this.http = http;
        this.clock = clock;
        this.metrics = metrics;
        this.providerId = providerId;
    }

    public String getAccessToken() {
        return getAccessToken(config.getTokenUrl());
    }

    public String getAccessToken(String tokenUrl) {
        String url = requireTokenUrl(tokenUrl);
        CachedToken snapshot = cachedByUrl.get(url);
        if (usable(snapshot)) {
            return snapshot.accessToken();
        }
        lock.lock();
        try {
            CachedToken locked = cachedByUrl.get(url);
            if (usable(locked)) {
                return locked.accessToken();
            }
            return refreshLocked(url);
        } finally {
            lock.unlock();
        }
    }

    public void invalidate() {
        lock.lock();
        try {
            cachedByUrl.clear();
        } finally {
            lock.unlock();
        }
    }

    public void invalidate(String tokenUrl) {
        if (!StringUtils.hasText(tokenUrl)) {
            return;
        }
        lock.lock();
        try {
            cachedByUrl.remove(tokenUrl.trim());
        } finally {
            lock.unlock();
        }
    }

    private String refreshLocked(String tokenUrl) {
        DataPowerHttpOutcome outcome;
        try {
            outcome = requestToken(tokenUrl);
        } catch (RuntimeException e) {
            metrics.incrementTokenRefreshFailure(providerId);
            throw new DataPowerGatewayException(DataPowerFailureClassifier.fromException("DataPower token", e));
        }
        if (outcome.statusCode() >= 200 && outcome.statusCode() < 300) {
            CachedToken token = parseToken(outcome.body());
            cachedByUrl.put(tokenUrl, token);
            metrics.incrementTokenRefreshSuccess(providerId);
            log.info("DataPower OAuth token refreshed host={} expires_in={}s",
                    DataPowerGatewayTargets.hostLabel(tokenUrl), token.expiresInSeconds());
            return token.accessToken();
        }
        metrics.incrementTokenRefreshFailure(providerId);
        throw new DataPowerGatewayException(classifyTokenHttp(outcome.statusCode()));
    }

    private DataPowerHttpOutcome requestToken(String tokenUrl) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        if (StringUtils.hasText(config.getScope())) {
            form.add("scope", config.getScope().trim());
        }
        return http.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .headers(headers -> headers.setBasicAuth(config.getClientId().trim(), config.getClientSecret()))
                .body(form)
                .exchange((request, response) -> DataPowerHttpOutcome.from(response));
    }

    private String requireTokenUrl(String tokenUrl) {
        if (StringUtils.hasText(tokenUrl)) {
            return tokenUrl.trim();
        }
        if (StringUtils.hasText(config.getTokenUrl())) {
            return config.getTokenUrl().trim();
        }
        throw new DataPowerGatewayException(ProviderResult.permanent("DataPower token URL is not configured"));
    }

    private CachedToken parseToken(String body) {
        DataPowerTokenResponse parsed;
        try {
            parsed = DataPowerHttpOutcome.readJson(body, DataPowerTokenResponse.class);
        } catch (RuntimeException e) {
            throw new DataPowerGatewayException(ProviderResult.retryable("DataPower token response was not valid JSON"));
        }
        if (parsed == null || !StringUtils.hasText(parsed.accessToken())) {
            throw new DataPowerGatewayException(ProviderResult.retryable("DataPower token response omitted access_token"));
        }
        long expiresIn = parsed.expiresIn() == null || parsed.expiresIn() <= 0
                ? DEFAULT_EXPIRES_IN_SECONDS
                : parsed.expiresIn();
        Instant expiresAt = clock.instant().plusSeconds(expiresIn);
        return new CachedToken(parsed.accessToken(), expiresAt, expiresIn);
    }

    private boolean usable(CachedToken token) {
        if (token == null || !StringUtils.hasText(token.accessToken())) {
            return false;
        }
        Instant refreshAt = token.expiresAt().minusSeconds(Math.max(0, config.getTokenRefreshSkewSeconds()));
        return clock.instant().isBefore(refreshAt);
    }

    private static ProviderResult classifyTokenHttp(int status) {
        if (status == 401 || status == 403) {
            return ProviderResult.permanent("DataPower token authentication failed");
        }
        if (status == 400 || status == 404) {
            return ProviderResult.permanent("DataPower token request was rejected (HTTP " + status + ")");
        }
        if (DataPowerFailureClassifier.retryableStatus(status)) {
            return ProviderResult.retryable("DataPower token endpoint HTTP " + status);
        }
        if (status >= 400 && status < 500) {
            return ProviderResult.permanent(truncate("DataPower token endpoint HTTP " + status));
        }
        return ProviderResult.retryable(truncate("DataPower token endpoint HTTP " + status));
    }

    static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > MAX_DETAIL ? value.substring(0, MAX_DETAIL) : value;
    }

    private record CachedToken(String accessToken, Instant expiresAt, long expiresInSeconds) {
    }

    /**
     * Visible for tests: whether the next {@link #getAccessToken()} can reuse the cache.
     */
    boolean hasFreshToken() {
        String url = StringUtils.hasText(config.getTokenUrl()) ? config.getTokenUrl().trim() : "";
        return usable(cachedByUrl.get(url));
    }
}
