package com.dashenbank.cms.customer;

import java.util.Map;

/**
 * Canonical complaint contact numbers. SMS and customer communication use
 * {@link #CURRENT_CONTACT_PHONE} only. Core Banking registered phone is display
 * reference data and must never be used as a fallback for notifications.
 */
public final class CustomerContactPhones {

    public static final String CURRENT_CONTACT_PHONE = "currentContactPhone";
    public static final String CORE_BANKING_PHONE = "coreBankingPhone";
    public static final String LEGACY_PHONE = "phone";
    public static final String PREFERRED_CONTACT_NUMBER = "preferredContactNumber";

    private CustomerContactPhones() {
    }

    public static String fromRequest(Map<String, ?> customer) {
        return firstNonBlank(
                value(customer, CURRENT_CONTACT_PHONE),
                value(customer, LEGACY_PHONE),
                value(customer, PREFERRED_CONTACT_NUMBER));
    }

    public static String currentContact(Map<String, ?> customer) {
        return fromRequest(customer);
    }

    public static String coreBanking(Map<String, ?> customer) {
        return firstNonBlank(
                value(customer, CORE_BANKING_PHONE),
                value(customer, "registeredPhone"));
    }

    public static void applyCurrentContact(Map<String, Object> customerVars, String currentContact) {
        String value = currentContact == null ? "" : currentContact;
        customerVars.put(CURRENT_CONTACT_PHONE, value);
        customerVars.put(LEGACY_PHONE, value);
    }

    public static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String value(Map<String, ?> customer, String key) {
        if (customer == null || key == null) {
            return null;
        }
        Object raw = customer.get(key);
        return raw == null ? null : raw.toString();
    }
}
