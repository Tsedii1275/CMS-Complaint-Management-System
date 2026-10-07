package com.dashenbank.cms.notification.provider.datapower;

import com.dashenbank.cms.notification.provider.ProviderResult;

/**
 * Transport failure already classified for the notification dispatcher.
 * Messages must never contain tokens, secrets, or Authorization headers.
 */
final class DataPowerGatewayException extends RuntimeException {

    private final ProviderResult result;

    DataPowerGatewayException(ProviderResult result) {
        super(result.detail());
        this.result = result;
    }

    ProviderResult result() {
        return result;
    }
}
