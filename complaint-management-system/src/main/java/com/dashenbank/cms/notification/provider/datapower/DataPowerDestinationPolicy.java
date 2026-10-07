package com.dashenbank.cms.notification.provider.datapower;

import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Gateway destination allow-list. Valid Ethiopian mobiles are not assumed to be
 * accepted by DataPower; Safaricom {@code 07} stays off until the SMS team adds
 * it to {@code SMS_DATAPOWER_ALLOWED_LOCAL_PREFIXES}.
 */
final class DataPowerDestinationPolicy {

    private DataPowerDestinationPolicy() {
    }

    static Set<String> parseAllowedLocalPrefixes(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new IllegalStateException(
                    "NOTIFICATION_SMS_PROVIDER=datapower requires SMS_DATAPOWER_ALLOWED_LOCAL_PREFIXES (09 and/or 07)");
        }
        Set<String> prefixes = Arrays.stream(raw.split(","))
                .map(value -> value == null ? "" : value.trim())
                .filter(value -> !value.isEmpty())
                .map(value -> value.startsWith("0") ? value : "0" + value)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (prefixes.isEmpty()) {
            throw new IllegalStateException(
                    "SMS_DATAPOWER_ALLOWED_LOCAL_PREFIXES must list 09 and/or 07");
        }
        for (String prefix : prefixes) {
            if (!EthiopianMobileNetwork.ETHIO_TELECOM.localPrefix().equals(prefix)
                    && !EthiopianMobileNetwork.SAFARICOM.localPrefix().equals(prefix)) {
                throw new IllegalStateException(
                        "SMS_DATAPOWER_ALLOWED_LOCAL_PREFIXES entries must be 09 or 07");
            }
        }
        return Set.copyOf(prefixes);
    }

    /**
     * @return a permanent-failure reason when the normalized local number is a
     *         valid Ethiopian mobile but the configured DataPower route has not
     *         enabled that network prefix
     */
    static Optional<String> rejectIfUnsupported(String localNumber, String allowedLocalPrefixes) {
        Set<String> allowed = parseAllowedLocalPrefixes(allowedLocalPrefixes);
        if (localNumber != null && localNumber.length() >= 2 && allowed.contains(localNumber.substring(0, 2))) {
            return Optional.empty();
        }
        String network = EthiopianMobileNetwork.fromLocal(localNumber)
                .map(value -> value.name().toLowerCase(Locale.ROOT).replace('_', ' '))
                .orElse("unknown");
        String prefix = localNumber != null && localNumber.length() >= 2 ? localNumber.substring(0, 2) : "";
        return Optional.of("DataPower SMS has not enabled " + network + " destinations (" + prefix
                + "); set SMS_DATAPOWER_ALLOWED_LOCAL_PREFIXES after the SMS team confirms the gateway");
    }
}
