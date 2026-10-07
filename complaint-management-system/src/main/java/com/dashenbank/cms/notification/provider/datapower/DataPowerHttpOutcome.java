package com.dashenbank.cms.notification.provider.datapower;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

record DataPowerHttpOutcome(int statusCode, String body) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static DataPowerHttpOutcome from(ClientHttpResponse response) throws IOException {
        int status = response.getStatusCode().value();
        byte[] bytes = response.getBody().readAllBytes();
        String body = bytes.length == 0 ? "" : new String(bytes, StandardCharsets.UTF_8);
        return new DataPowerHttpOutcome(status, body);
    }

    static <T> T readJson(String body, Class<T> type) {
        try {
            return MAPPER.readValue(body == null ? "" : body, type);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid JSON", e);
        }
    }

    static com.fasterxml.jackson.databind.JsonNode readTree(String body) {
        try {
            return MAPPER.readTree(body == null ? "" : body);
        } catch (IOException e) {
            return null;
        }
    }
}
