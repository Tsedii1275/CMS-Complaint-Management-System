package com.dashenbank.cms.notification.provider.datapower;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DataPowerTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("expires_in") Long expiresIn,
        @JsonProperty("token_type") String tokenType) {
}
