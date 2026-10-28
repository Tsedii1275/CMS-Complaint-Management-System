package com.dashenbank.cms.service;

import java.util.Locale;
import java.util.Map;

/**
 * Sub-types of an item screened as "Other" (non-complaint).
 * <ul>
 * <li>DISPUTE: listed on the Contact Center "Other" tab.</li>
 * <li>FEEDBACK: listed on the Service Quality "Feedback" tab.</li>
 * <li>ETHICS: listed on the Ethics and Conduct dashboard.</li>
 * </ul>
 * All three keep classification/status OTHER so they stay out of complaint analytics and reports.
 */
public final class OtherClassification {

    public static final String KEY_OTHER_TYPE = "otherType";
    public static final String DISPUTE = "DISPUTE";
    public static final String FEEDBACK = "FEEDBACK";
    public static final String ETHICS = "ETHICS";

    private OtherClassification() {
    }

    /**
     * A blank or unknown value stays a DISPUTE so items screened before the later
     * splits keep the Contact Center Other tab.
     */
    public static String normalize(Object raw) {
        if (raw == null) {
            return DISPUTE;
        }
        String value = raw.toString().trim().toUpperCase(Locale.ROOT);
        if (FEEDBACK.equals(value)) {
            return FEEDBACK;
        }
        if (ETHICS.equals(value) || "ETHICS AND CONDUCT".equals(value)) {
            return ETHICS;
        }
        return DISPUTE;
    }

    public static boolean isFeedback(Object raw) {
        return FEEDBACK.equals(normalize(raw));
    }

    public static boolean isEthics(Object raw) {
        return ETHICS.equals(normalize(raw));
    }

    public static String typeOf(Map<String, Object> vars) {
        return normalize(vars == null ? null : vars.get(KEY_OTHER_TYPE));
    }

    public static String targetTab(String otherType) {
        if (FEEDBACK.equals(otherType)) {
            return "Feedback";
        }
        if (ETHICS.equals(otherType)) {
            return "Ethics";
        }
        return "Other";
    }

    public static String label(String otherType) {
        if (FEEDBACK.equals(otherType)) {
            return "Feedback";
        }
        if (ETHICS.equals(otherType)) {
            return "Ethics and Conduct";
        }
        return "Dispute";
    }
}
