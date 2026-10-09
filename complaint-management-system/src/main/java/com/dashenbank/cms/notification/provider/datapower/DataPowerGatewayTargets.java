package com.dashenbank.cms.notification.provider.datapower;

import com.dashenbank.cms.notification.NotificationProperties;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Primary DataPower node plus optional fallback pair. Token and send stay on
 * the same host so an OAuth token is not reused against a different gateway.
 */
final class DataPowerGatewayTargets {

    private DataPowerGatewayTargets() {
    }

    static List<Target> of(NotificationProperties.DataPower config) {
        List<Target> targets = new ArrayList<>();
        if (config != null) {
            addIfComplete(targets, config.getTokenUrl(), config.getSendUrl());
            addIfComplete(targets, config.getFallbackTokenUrl(), config.getFallbackSendUrl());
        }
        return List.copyOf(targets);
    }

    static String hostLabel(String url) {
        if (!StringUtils.hasText(url)) {
            return "";
        }
        try {
            URI uri = URI.create(url.trim());
            String host = uri.getHost() == null ? "" : uri.getHost();
            int port = uri.getPort();
            return port > 0 ? host + ":" + port : host;
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    static boolean shouldTryNextHost(int status) {
        return status == 502 || status == 503 || status == 504;
    }

    private static void addIfComplete(List<Target> targets, String tokenUrl, String sendUrl) {
        if (!StringUtils.hasText(tokenUrl) || !StringUtils.hasText(sendUrl)) {
            return;
        }
        Target next = new Target(tokenUrl.trim(), sendUrl.trim());
        for (Target existing : targets) {
            if (existing.tokenUrl().equals(next.tokenUrl()) && existing.sendUrl().equals(next.sendUrl())) {
                return;
            }
        }
        targets.add(next);
    }

    record Target(String tokenUrl, String sendUrl) {
    }
}
