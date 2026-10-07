package com.dashenbank.cms.notification.provider.datapower;

import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Ethiopian mobile validation and DataPower destination formatting.
 *
 * <p>Validity includes both Ethio Telecom ({@code 09}/{@code 9}) and Safaricom
 * Ethiopia ({@code 07}/{@code 7}). That is not the same as DataPower accepting
 * the destination; {@link DataPowerDestinationPolicy} applies gateway prefixes
 * separately.
 *
 * <p>{@code SMS_DATAPOWER_PHONE_FORMAT=LOCAL_09} means local {@code 0X} form for
 * the matching network: {@code 09XXXXXXXX} or {@code 07XXXXXXXX}. Safaricom
 * numbers are never rewritten to {@code 09}.
 */
public final class DataPowerPhoneNormalizer {

    public static final String LOCAL_09 = "LOCAL_09";

    private static final Pattern SEPARATORS = Pattern.compile("[\\s().-]");
    private static final Pattern LOCAL = Pattern.compile("^(09\\d{8}|07\\d{8})$");
    private static final Pattern INTERNATIONAL = Pattern.compile("^\\+251[79]\\d{8}$");
    private static final Pattern COUNTRY_CODE = Pattern.compile("^251[79]\\d{8}$");

    private DataPowerPhoneNormalizer() {
    }

    public static boolean supports(String phoneFormat) {
        return LOCAL_09.equalsIgnoreCase(trim(phoneFormat));
    }

    /**
     * Validates an Ethiopian mobile and, for {@code LOCAL_09}, returns the local
     * {@code 0[79]XXXXXXXX} destination. Empty if the number is not a valid
     * Ethio Telecom or Safaricom Ethiopia mobile.
     */
    public static Optional<String> toLocalFormat(String raw) {
        String compact = compact(raw);
        if (compact == null) {
            return Optional.empty();
        }
        if (LOCAL.matcher(compact).matches()) {
            return Optional.of(compact);
        }
        if (INTERNATIONAL.matcher(compact).matches()) {
            return Optional.of("0" + compact.substring(4));
        }
        if (COUNTRY_CODE.matcher(compact).matches()) {
            return Optional.of("0" + compact.substring(3));
        }
        if (compact.startsWith("00")) {
            String plusForm = "+" + compact.substring(2);
            if (INTERNATIONAL.matcher(plusForm).matches()) {
                return Optional.of("0" + plusForm.substring(4));
            }
        }
        return Optional.empty();
    }

    static Optional<EthiopianMobileNetwork> networkOf(String localNumber) {
        return EthiopianMobileNetwork.fromLocal(localNumber);
    }

    private static String compact(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String trimmed = SEPARATORS.matcher(raw.trim()).replaceAll("");
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
