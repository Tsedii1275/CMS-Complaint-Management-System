package com.dashenbank.cms.customer;

import java.util.Map;

/**
 * Explicit location keys for Core Banking home vs operational complaint
 * geography. New process logic must not write generic {@code branch} or
 * {@code district} onto customer or complaint maps.
 */
public final class LocationKeys {

    public static final String CUSTOMER_HOME_BRANCH = "customerHomeBranch";
    public static final String CUSTOMER_HOME_DISTRICT = "customerHomeDistrict";
    public static final String COMPLAINT_BRANCH = "complaintBranch";
    public static final String COMPLAINT_DISTRICT = "complaintDistrict";
    public static final String LEGACY_BRANCH = "branch";
    public static final String LEGACY_DISTRICT = "district";
    public static final String LEGACY_HOME_BRANCH = "homeBranch";

    private LocationKeys() {
    }

    public static String customerHomeBranch(Map<String, ?> customer) {
        return firstNonBlank(value(customer, CUSTOMER_HOME_BRANCH), value(customer, LEGACY_HOME_BRANCH));
    }

    public static String customerHomeDistrict(Map<String, ?> customer) {
        return firstNonBlank(value(customer, CUSTOMER_HOME_DISTRICT), value(customer, LEGACY_DISTRICT));
    }

    public static String complaintBranch(Map<String, ?> vars, Map<String, ?> complaint) {
        return firstNonBlank(
                value(complaint, COMPLAINT_BRANCH),
                value(vars, COMPLAINT_BRANCH),
                value(complaint, LEGACY_BRANCH),
                value(vars, LEGACY_BRANCH));
    }

    public static String complaintDistrict(Map<String, ?> vars, Map<String, ?> complaint) {
        return firstNonBlank(
                value(complaint, COMPLAINT_DISTRICT),
                value(vars, COMPLAINT_DISTRICT),
                value(complaint, LEGACY_DISTRICT),
                value(vars, LEGACY_DISTRICT));
    }

    public static String fromRequestBranch(Map<String, ?> complaint) {
        return firstNonBlank(value(complaint, COMPLAINT_BRANCH), value(complaint, LEGACY_BRANCH));
    }

    public static String fromRequestDistrict(Map<String, ?> complaint) {
        return firstNonBlank(value(complaint, COMPLAINT_DISTRICT), value(complaint, LEGACY_DISTRICT));
    }

    public static void applyCustomerHome(Map<String, Object> customerVars, String homeBranch, String homeDistrict) {
        putIfHasText(customerVars, CUSTOMER_HOME_BRANCH, homeBranch);
        putIfHasText(customerVars, CUSTOMER_HOME_DISTRICT, homeDistrict);
    }

    public static void applyComplaintLocation(Map<String, Object> complaintVars, String branch, String district) {
        putIfHasText(complaintVars, COMPLAINT_BRANCH, branch);
        putIfHasText(complaintVars, COMPLAINT_DISTRICT, district);
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

    private static void putIfHasText(Map<String, Object> target, String key, String value) {
        if (target == null || key == null || value == null || value.isBlank()) {
            return;
        }
        target.put(key, value);
    }

    private static String value(Map<String, ?> map, String key) {
        if (map == null || key == null) {
            return null;
        }
        Object raw = map.get(key);
        return raw == null ? null : raw.toString();
    }
}
