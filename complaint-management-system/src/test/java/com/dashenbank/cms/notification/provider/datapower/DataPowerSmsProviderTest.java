package com.dashenbank.cms.notification.provider.datapower;

import com.dashenbank.cms.notification.NotificationProperties;
import com.dashenbank.cms.notification.provider.ProviderResult;
import com.dashenbank.cms.notification.provider.SmsMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataPowerSmsProviderTest {

    private static final Instant NOON_UTC = Instant.parse("2026-10-06T09:00:00Z");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private DataPowerGatewayStub gateway;
    private SimpleMeterRegistry registry;
    private MutableClock clock;
    private DataPowerSmsProvider provider;

    @BeforeEach
    void setUp() throws Exception {
        gateway = new DataPowerGatewayStub();
        registry = new SimpleMeterRegistry();
        clock = new MutableClock(NOON_UTC);
        provider = newProvider(configured(gateway));
    }

    @AfterEach
    void tearDown() {
        gateway.close();
    }

    @Test
    void sendsOfficialPayloadAndCachesToken() throws Exception {
        ProviderResult first = provider.send(sms("+251912345678"));
        ProviderResult second = provider.send(sms("0912345678"));

        assertEquals(ProviderResult.Outcome.DELIVERED, first.outcome());
        assertEquals("sms-99", first.providerMessageId());
        assertEquals(ProviderResult.Outcome.DELIVERED, second.outcome());
        assertEquals(1, gateway.tokenCalls.get());
        assertEquals(2, gateway.sendCalls.get());
        assertTrue(gateway.lastTokenAuthorization.get().startsWith("Basic "));
        assertTrue(gateway.lastTokenForm.get().contains("grant_type=client_credentials"));
        assertTrue(gateway.lastTokenForm.get().contains("scope=DASHEN"));
        assertTrue(gateway.lastSendAuthorization.get().startsWith("Bearer "));
        assertFalse(gateway.lastSendJson.get().contains("tok-1"));
        assertFalse(gateway.lastSendJson.get().contains("sms-secret"));

        JsonNode body = MAPPER.readTree(gateway.lastSendJson.get());
        assertEquals("Voice Management", body.get("sendFor").asText());
        assertEquals("0912345678", body.get("phoneNumber").asText());
        assertEquals("2026-10-06", body.get("sendDate").asText());
        assertEquals("complaint registered", body.get("message").asText());
        assertEquals(2.0, counter(SmsMetrics.SEND_SUCCESS));
        assertEquals(1.0, counter(SmsMetrics.TOKEN_REFRESH_SUCCESS));
        assertTrue(registry.get(SmsMetrics.PROVIDER_LATENCY).timer().count() >= 2);
    }

    @Test
    void sendDateUsesAfricaAddisAbaba() {
        clock.set(Instant.parse("2026-10-06T21:00:00Z"));
        provider.send(sms("0912345678"));
        assertTrue(gateway.lastSendJson.get().contains("\"sendDate\":\"2026-10-07\""));
        assertEquals("2026-10-07", provider.sendDate());
    }

    @Test
    void refreshesTokenWhenSkewWindowReached() {
        gateway.tokenBody = "{\"access_token\":\"tok-1\",\"expires_in\":120,\"token_type\":\"Bearer\"}";
        provider.send(sms("0912345678"));
        assertEquals(1, gateway.tokenCalls.get());

        clock.advance(Duration.ofSeconds(61));
        provider.send(sms("0912345678"));
        assertEquals(2, gateway.tokenCalls.get());
    }

    @Test
    void tokenEndpoint500IsRetryable() {
        gateway.tokenStatus = 500;
        ProviderResult result = provider.send(sms("0912345678"));
        assertEquals(ProviderResult.Outcome.RETRYABLE_FAILURE, result.outcome());
        assertEquals(0, gateway.sendCalls.get());
        assertEquals(1.0, counter(SmsMetrics.TOKEN_REFRESH_FAILURE));
        assertEquals(1.0, counter(SmsMetrics.SEND_RETRYABLE_FAILURE));
    }

    @Test
    void tokenEndpointTimeoutIsRetryable() {
        gateway.tokenSleepMs = 1500;
        NotificationProperties properties = configured(gateway);
        properties.getSms().getDatapower().setReadTimeoutMs(200);
        properties.getSms().getDatapower().setConnectTimeoutMs(200);
        provider = newProvider(properties);

        ProviderResult result = provider.send(sms("0912345678"));
        assertEquals(ProviderResult.Outcome.RETRYABLE_FAILURE, result.outcome());
        assertTrue(result.detail().toLowerCase().contains("timeout")
                || result.detail().contains("Timeout")
                || result.detail().contains("HttpTimeout")
                || result.detail().contains("SocketTimeout")
                || result.detail().contains("connection failure"));
        assertEquals(1.0, counter(SmsMetrics.TOKEN_REFRESH_FAILURE));
    }

    @Test
    void refreshesOnceOn401ThenSucceeds() {
        gateway.sendStatusSequence.add(401);
        gateway.sendStatusSequence.add(200);

        ProviderResult result = provider.send(sms("0912345678"));

        assertEquals(ProviderResult.Outcome.DELIVERED, result.outcome());
        assertEquals(2, gateway.tokenCalls.get());
        assertEquals(2, gateway.sendCalls.get());
        assertEquals(1.0, counter(SmsMetrics.SEND_SUCCESS));
    }

    @Test
    void two401ResponsesArePermanentAuthenticationFailure() {
        gateway.sendStatus = 401;

        ProviderResult result = provider.send(sms("0912345678"));

        assertEquals(ProviderResult.Outcome.PERMANENT_FAILURE, result.outcome());
        assertTrue(result.detail().toLowerCase().contains("authentication"));
        assertEquals(2, gateway.tokenCalls.get());
        assertEquals(2, gateway.sendCalls.get());
        assertEquals(1.0, counter(SmsMetrics.SEND_FAILURE));
        assertEquals(0.0, counterOrZero(SmsMetrics.SEND_RETRYABLE_FAILURE));
    }

    @Test
    void http400And403ArePermanent() {
        gateway.sendStatus = 400;
        assertEquals(ProviderResult.Outcome.PERMANENT_FAILURE, provider.send(sms("0912345678")).outcome());
        gateway.sendStatus = 403;
        assertEquals(ProviderResult.Outcome.PERMANENT_FAILURE, provider.send(sms("251912345678")).outcome());
        assertEquals(2.0, counter(SmsMetrics.SEND_FAILURE));
    }

    @Test
    void http429IsRetryable() {
        gateway.sendStatus = 429;
        ProviderResult result = provider.send(sms("0912345678"));
        assertEquals(ProviderResult.Outcome.RETRYABLE_FAILURE, result.outcome());
        assertEquals(1.0, counter(SmsMetrics.SEND_RETRYABLE_FAILURE));
    }

    @Test
    void http500IsRetryable() {
        gateway.sendStatus = 500;
        ProviderResult result = provider.send(sms("0912345678"));
        assertEquals(ProviderResult.Outcome.RETRYABLE_FAILURE, result.outcome());
        assertEquals(1.0, counter(SmsMetrics.SEND_RETRYABLE_FAILURE));
    }

    @Test
    void sendTimeoutIsRetryable() {
        gateway.sendSleepMs = 1500;
        NotificationProperties properties = configured(gateway);
        properties.getSms().getDatapower().setReadTimeoutMs(200);
        properties.getSms().getDatapower().setConnectTimeoutMs(200);
        provider = newProvider(properties);

        ProviderResult result = provider.send(sms("0912345678"));
        assertEquals(ProviderResult.Outcome.RETRYABLE_FAILURE, result.outcome());
        assertEquals(1.0, counter(SmsMetrics.SEND_RETRYABLE_FAILURE));
    }

    @Test
    void invalidPhoneIsPermanentAndDoesNotCallGateway() {
        ProviderResult result = provider.send(sms("0812345678"));
        assertEquals(ProviderResult.Outcome.PERMANENT_FAILURE, result.outcome());
        assertEquals(0, gateway.tokenCalls.get());
        assertEquals(0, gateway.sendCalls.get());
        assertEquals(1.0, counter(SmsMetrics.SEND_FAILURE));
    }

    @Test
    void safaricomIsValidButBlockedUntilGatewayPrefixesInclude07() {
        ProviderResult result = provider.send(sms("+251712345678"));
        assertEquals(ProviderResult.Outcome.PERMANENT_FAILURE, result.outcome());
        assertTrue(result.detail().contains("SMS_DATAPOWER_ALLOWED_LOCAL_PREFIXES"));
        assertEquals(0, gateway.tokenCalls.get());
        assertEquals(0, gateway.sendCalls.get());
    }

    @Test
    void sendsSafaricomAs07WhenGatewayPrefixesInclude07() throws Exception {
        NotificationProperties properties = configured(gateway);
        properties.getSms().getDatapower().setAllowedLocalPrefixes("09,07");
        provider = newProvider(properties);

        ProviderResult result = provider.send(sms("+251712345678"));

        assertEquals(ProviderResult.Outcome.DELIVERED, result.outcome());
        JsonNode body = MAPPER.readTree(gateway.lastSendJson.get());
        assertEquals("0712345678", body.get("phoneNumber").asText());
        assertFalse(body.get("phoneNumber").asText().startsWith("09"));
    }

    @Test
    void destinationNetworkRejectionFollowsHttpRetryPolicy() {
        gateway.sendStatus = 400;
        gateway.sendBody = "{\"error\":\"unsupported network / invalid destination\"}";
        ProviderResult result = provider.send(sms("0912345678"));
        assertEquals(ProviderResult.Outcome.PERMANENT_FAILURE, result.outcome());
        assertTrue(result.detail().contains("destination/network"));
        assertFalse(result.detail().contains("unsupported network / invalid destination"));
    }

    @Test
    void singleTokenRefreshUnderLoad() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(16);
        try {
            List<Callable<String>> tasks = new ArrayList<>();
            for (int i = 0; i < 32; i++) {
                tasks.add(() -> provider.tokenClient().getAccessToken());
            }
            for (Future<String> future : pool.invokeAll(tasks)) {
                assertEquals("tok-1", future.get(5, TimeUnit.SECONDS));
            }
            assertEquals(1, gateway.tokenCalls.get());

            clock.advance(Duration.ofSeconds(3600));
            gateway.tokenBody = "{\"access_token\":\"tok-2\",\"expires_in\":3600,\"token_type\":\"Bearer\"}";
            List<Callable<String>> refresh = new ArrayList<>();
            for (int i = 0; i < 32; i++) {
                refresh.add(() -> provider.tokenClient().getAccessToken());
            }
            for (Future<String> future : pool.invokeAll(refresh)) {
                assertEquals("tok-2", future.get(5, TimeUnit.SECONDS));
            }
        } finally {
            pool.shutdownNow();
        }
        assertEquals(2, gateway.tokenCalls.get());
        assertTrue(provider.tokenClient().hasFreshToken());
    }

    @Test
    void doesNotLogSecretsInFailureDetail() {
        gateway.sendStatus = 400;
        gateway.sendBody = "{\"error\":\"denied\",\"access_token\":\"should-not-leak\"}";
        ProviderResult result = provider.send(sms("0912345678"));
        assertFalse(result.detail().contains("should-not-leak"));
        assertFalse(result.detail().contains("sms-secret"));
        assertFalse(result.detail().contains("Bearer "));
    }

    private DataPowerSmsProvider newProvider(NotificationProperties properties) {
        RestClient http = DataPowerHttpFactory.createRestClient(properties.getSms().getDatapower());
        return new DataPowerSmsProvider(properties, new SmsMetrics(registry), clock, http);
    }

    private static NotificationProperties configured(DataPowerGatewayStub gateway) {
        NotificationProperties properties = new NotificationProperties();
        NotificationProperties.DataPower datapower = properties.getSms().getDatapower();
        datapower.setTokenUrl(gateway.tokenUrl());
        datapower.setSendUrl(gateway.sendUrl());
        datapower.setClientId("sms-client");
        datapower.setClientSecret("sms-secret");
        datapower.setScope("DASHEN");
        datapower.setSendFor("Voice Management");
        datapower.setPhoneFormat(DataPowerPhoneNormalizer.LOCAL_09);
        datapower.setAllowedLocalPrefixes("09");
        datapower.setConnectTimeoutMs(2000);
        datapower.setReadTimeoutMs(2000);
        datapower.setTokenRefreshSkewSeconds(60);
        return properties;
    }

    private static SmsMessage sms(String to) {
        return new SmsMessage(to, "", "complaint registered");
    }

    private double counter(String name) {
        return registry.get(name).tag("provider", DataPowerSmsProvider.ID).counter().count();
    }

    private double counterOrZero(String name) {
        try {
            return counter(name);
        } catch (RuntimeException e) {
            return 0;
        }
    }
}
