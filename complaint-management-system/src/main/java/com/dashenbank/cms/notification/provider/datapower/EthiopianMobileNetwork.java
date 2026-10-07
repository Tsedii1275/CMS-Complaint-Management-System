package com.dashenbank.cms.notification.provider.datapower;

import java.util.Optional;

/**
 * Ethiopian mobile networks identifiable from the national prefix. Not a
 * DataPower capability flag.
 */
enum EthiopianMobileNetwork {

    ETHIO_TELECOM("09"),
    SAFARICOM("07");

    private final String localPrefix;

    EthiopianMobileNetwork(String localPrefix) {
        this.localPrefix = localPrefix;
    }

    String localPrefix() {
        return localPrefix;
    }

    static Optional<EthiopianMobileNetwork> fromLocal(String localNumber) {
        if (localNumber == null || localNumber.length() < 2) {
            return Optional.empty();
        }
        String prefix = localNumber.substring(0, 2);
        if (ETHIO_TELECOM.localPrefix.equals(prefix)) {
            return Optional.of(ETHIO_TELECOM);
        }
        if (SAFARICOM.localPrefix.equals(prefix)) {
            return Optional.of(SAFARICOM);
        }
        return Optional.empty();
    }
}
