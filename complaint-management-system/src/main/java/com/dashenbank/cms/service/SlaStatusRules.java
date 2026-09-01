package com.dashenbank.cms.service;

import com.dashenbank.cms.model.ComplaintSlaMetrics;

/**
 * Single definition of SLA status used by analytics and reporting.
 * Status values are produced only by {@link SlaTrackingService#recalculateSlaStatus}.
 */
public final class SlaStatusRules {

    private SlaStatusRules() {
    }

    public static boolean isBreached(ComplaintSlaMetrics m) {
        if (m == null) {
            return false;
        }
        String status = m.getSlaStatus();
        return Boolean.TRUE.equals(m.getBreached())
                || "BREACHED".equalsIgnoreCase(status)
                || "RESOLVED_AFTER_SLA".equalsIgnoreCase(status);
    }

    public static boolean isApproaching(ComplaintSlaMetrics m) {
        return m != null && "APPROACHING".equalsIgnoreCase(m.getSlaStatus());
    }

    public static boolean isResolvedWithinSla(ComplaintSlaMetrics m) {
        return m != null && "RESOLVED_WITHIN_SLA".equalsIgnoreCase(m.getSlaStatus());
    }

    public static boolean isClosedForCompliance(ComplaintSlaMetrics m) {
        if (m == null || m.getStatus() == null) {
            return false;
        }
        String status = m.getStatus();
        return "CLOSED".equalsIgnoreCase(status) || "RESOLVED".equalsIgnoreCase(status);
    }
}
