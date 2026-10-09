package com.dashenbank.cms.notification.provider.datapower;

import com.dashenbank.cms.notification.NotificationProperties;
import com.dashenbank.cms.notification.RecipientFormat;
import com.dashenbank.cms.notification.provider.ProviderResult;
import com.dashenbank.cms.notification.provider.SmsMessage;
import com.dashenbank.cms.notification.provider.SmsProvider;
import com.fasterxml.jackson.databind.JsonNode;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Dashen DataPower SMS transport. Selected with {@code NOTIFICATION_SMS_PROVIDER=datapower}.
 * Controllers and Flowable delegates must not call this class; the dispatcher does.
 */
@Component
public class DataPowerSmsProvider implements SmsProvider {

    public static final String ID = "datapower";
    static final ZoneId ADDIS_ABABA = ZoneId.of("Africa/Addis_Ababa");
    private static final DateTimeFormatter SEND_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final List<String> MESSAGE_ID_FIELDS = List.of(
            "messageId", "message_id", "id", "requestId", "transactionId", "smsId");
    private static final Logger log = LoggerFactory.getLogger(DataPowerSmsProvider.class);

    private final NotificationProperties properties;
    private final SmsMetrics metrics;
    private final Clock clock;
    private final Object lock = new Object();

    private RestClient http;
    private DataPowerOAuthTokenClient tokens;

    @Autowired
    public DataPowerSmsProvider(NotificationProperties properties, SmsMetrics metrics) {
        this(properties, metrics, Clock.systemUTC(), null);
    }

    DataPowerSmsProvider(NotificationProperties properties, SmsMetrics metrics, Clock clock, RestClient http) {
        this.properties = properties;
        this.metrics = metrics;
        this.clock = clock;
        this.http = http;
        if (http != null) {
            this.tokens = new DataPowerOAuthTokenClient(properties.getSms().getDatapower(), http, clock, metrics, ID);
        }
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void validateConfiguration() {
        NotificationProperties.DataPower config = datapower();
        require(config.getTokenUrl(), "SMS_DATAPOWER_TOKEN_URL");
        require(config.getSendUrl(), "SMS_DATAPOWER_SEND_URL");
        require(config.getClientId(), "SMS_DATAPOWER_CLIENT_ID");
        require(config.getClientSecret(), "SMS_DATAPOWER_CLIENT_SECRET");
        require(config.getScope(), "SMS_DATAPOWER_SCOPE");
        require(config.getSendFor(), "SMS_DATAPOWER_SEND_FOR");
        if (!DataPowerPhoneNormalizer.supports(config.getPhoneFormat())) {
            throw new IllegalStateException(
                    "SMS_DATAPOWER_PHONE_FORMAT must be LOCAL_09 (local 0X form for 09 and 07)");
        }
        DataPowerDestinationPolicy.parseAllowedLocalPrefixes(config.getAllowedLocalPrefixes());
        DataPowerHttpFactory.validateTruststore(config);
        ensureClients();
    }

    @Override
    public ProviderResult send(SmsMessage message) {
        Timer.Sample latency = metrics.startLatency();
        try {
            return record(doSend(message));
        } catch (DataPowerGatewayException e) {
            return record(e.result());
        } catch (RuntimeException e) {
            return record(DataPowerFailureClassifier.fromException("DataPower SMS", e));
        } finally {
            metrics.stopLatency(latency, ID);
        }
    }

    private ProviderResult doSend(SmsMessage message) {
        if (message == null || !StringUtils.hasText(message.to())) {
            return ProviderResult.permanent("Invalid Ethiopian mobile number");
        }
        Optional<String> phone = DataPowerPhoneNormalizer.toLocalFormat(message.to());
        if (phone.isEmpty()) {
            return ProviderResult.permanent("Invalid Ethiopian mobile number");
        }
        NotificationProperties.DataPower config = datapower();
        Optional<String> unsupported = DataPowerDestinationPolicy.rejectIfUnsupported(
                phone.get(), config.getAllowedLocalPrefixes());
        if (unsupported.isPresent()) {
            log.warn("DataPower SMS skipped for {} : gateway has not enabled that network prefix",
                    RecipientFormat.maskPhone(phone.get()));
            return ProviderResult.permanent(unsupported.get());
        }
        DataPowerSmsRequest request = new DataPowerSmsRequest(
                config.getSendFor(),
                message.body() == null ? "" : message.body(),
                phone.get(),
                sendDate());
        ensureClients();
        List<DataPowerGatewayTargets.Target> targets = DataPowerGatewayTargets.of(config);
        if (targets.isEmpty()) {
            return ProviderResult.permanent("DataPower SMS URLs are not configured");
        }
        DataPowerGatewayException lastFailure = null;
        DataPowerHttpOutcome lastRetryable = null;
        for (int i = 0; i < targets.size(); i++) {
            DataPowerGatewayTargets.Target target = targets.get(i);
            try {
                DataPowerHttpOutcome outcome = postSmsOnTarget(target, request);
                if (outcome.statusCode() >= 200 && outcome.statusCode() < 300) {
                    if (i > 0) {
                        log.info("DataPower SMS accepted on fallback host {}",
                                DataPowerGatewayTargets.hostLabel(target.sendUrl()));
                    }
                    return classifySend(outcome, phone.get());
                }
                if (outcome.statusCode() == 401) {
                    return classifySend(outcome, phone.get());
                }
                if (i + 1 < targets.size() && DataPowerGatewayTargets.shouldTryNextHost(outcome.statusCode())) {
                    log.warn("DataPower SMS host {} returned HTTP {}; trying fallback",
                            DataPowerGatewayTargets.hostLabel(target.sendUrl()), outcome.statusCode());
                    lastRetryable = outcome;
                    continue;
                }
                return classifySend(outcome, phone.get());
            } catch (DataPowerGatewayException e) {
                lastFailure = e;
                if (i + 1 < targets.size() && e.result() != null
                        && e.result().outcome() == ProviderResult.Outcome.RETRYABLE_FAILURE) {
                    log.warn("DataPower SMS host {} unreachable; trying fallback",
                            DataPowerGatewayTargets.hostLabel(target.sendUrl()));
                    continue;
                }
                throw e;
            }
        }
        if (lastRetryable != null) {
            return classifySend(lastRetryable, phone.get());
        }
        if (lastFailure != null) {
            throw lastFailure;
        }
        return ProviderResult.retryable("DataPower SMS no reachable gateway");
    }

    private DataPowerHttpOutcome postSmsOnTarget(DataPowerGatewayTargets.Target target, DataPowerSmsRequest request) {
        DataPowerHttpOutcome first = postSms(target.sendUrl(), tokens.getAccessToken(target.tokenUrl()), request);
        if (first.statusCode() != 401) {
            return first;
        }
        tokens.invalidate(target.tokenUrl());
        DataPowerHttpOutcome second = postSms(target.sendUrl(), tokens.getAccessToken(target.tokenUrl()), request);
        if (second.statusCode() == 401) {
            log.warn("DataPower SMS authentication failed after token refresh on {}",
                    DataPowerGatewayTargets.hostLabel(target.sendUrl()));
        }
        return second;
    }

    private DataPowerHttpOutcome postSms(String sendUrl, String accessToken, DataPowerSmsRequest request) {
        try {
            return http.post()
                    .uri(sendUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(request)
                    .exchange((req, response) -> DataPowerHttpOutcome.from(response));
        } catch (DataPowerGatewayException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new DataPowerGatewayException(DataPowerFailureClassifier.fromException("DataPower SMS", e));
        }
    }

    private ProviderResult classifySend(DataPowerHttpOutcome outcome, String phone) {
        String masked = RecipientFormat.maskPhone(phone);
        String network = DataPowerPhoneNormalizer.networkOf(phone).map(Enum::name).orElse("UNKNOWN");
        if (outcome.statusCode() >= 200 && outcome.statusCode() < 300) {
            String messageId = extractMessageId(outcome.body());
            log.info("DataPower SMS accepted to {} network={} http={}", masked, network, outcome.statusCode());
            return ProviderResult.delivered(messageId);
        }
        ProviderResult result = DataPowerFailureClassifier.fromHttp(outcome.statusCode(), "DataPower SMS",
                outcome.body());
        log.warn("DataPower SMS failed to {} network={} http={} retryable={}", masked, network, outcome.statusCode(),
                result.outcome() == ProviderResult.Outcome.RETRYABLE_FAILURE);
        return result;
    }

    private ProviderResult record(ProviderResult result) {
        if (result == null) {
            metrics.incrementSendRetryableFailure(ID);
            return ProviderResult.retryable("DataPower SMS returned no result");
        }
        switch (result.outcome()) {
            case DELIVERED -> metrics.incrementSendSuccess(ID);
            case RETRYABLE_FAILURE -> metrics.incrementSendRetryableFailure(ID);
            case PERMANENT_FAILURE, NOT_DELIVERED -> metrics.incrementSendFailure(ID);
        }
        return result;
    }

    String sendDate() {
        return SEND_DATE.format(clock.instant().atZone(ADDIS_ABABA));
    }

    DataPowerOAuthTokenClient tokenClient() {
        ensureClients();
        return tokens;
    }

    private void ensureClients() {
        if (http != null && tokens != null) {
            return;
        }
        synchronized (lock) {
            if (http == null) {
                http = DataPowerHttpFactory.createRestClient(datapower());
            }
            if (tokens == null) {
                tokens = new DataPowerOAuthTokenClient(datapower(), http, clock, metrics, ID);
            }
        }
    }

    private NotificationProperties.DataPower datapower() {
        return properties.getSms().getDatapower();
    }

    private static void require(String value, String envName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("NOTIFICATION_SMS_PROVIDER=datapower requires " + envName);
        }
    }

    private static String extractMessageId(String body) {
        if (!StringUtils.hasText(body)) {
            return null;
        }
        JsonNode node = DataPowerHttpOutcome.readTree(body);
        if (node == null || node.isMissingNode() || node.isNull()) {
            return body.length() <= 80 && !looksSensitive(body) ? body.trim() : null;
        }
        for (String field : MESSAGE_ID_FIELDS) {
            JsonNode value = node.get(field);
            if (value != null && value.isValueNode() && StringUtils.hasText(value.asText())) {
                String id = value.asText().trim();
                return id.length() > 120 ? id.substring(0, 120) : id;
            }
        }
        return null;
    }

    private static boolean looksSensitive(String body) {
        String lower = body.toLowerCase();
        return lower.contains("access_token") || lower.contains("bearer ") || lower.contains("authorization");
    }
}
