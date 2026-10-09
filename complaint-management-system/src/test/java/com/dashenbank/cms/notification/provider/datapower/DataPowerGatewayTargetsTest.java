package com.dashenbank.cms.notification.provider.datapower;

import com.dashenbank.cms.notification.NotificationProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataPowerGatewayTargetsTest {

    @Test
    void primaryThenFallbackStayPairedByHost() {
        NotificationProperties.DataPower config = new NotificationProperties.DataPower();
        config.setTokenUrl("https://10.41.30.114:9443/dashen-bank/sandbox/oauth19/oauth2/token");
        config.setSendUrl("https://10.41.30.114:9443/dashen-bank/sandbox/SMS/send");
        config.setFallbackTokenUrl("https://10.0.20.68:9443/dashen-bank/sandbox/oauth19/oauth2/token");
        config.setFallbackSendUrl("https://10.0.20.68:9443/dashen-bank/sandbox/SMS/send");
        List<DataPowerGatewayTargets.Target> targets = DataPowerGatewayTargets.of(config);
        assertEquals(2, targets.size());
        assertEquals("10.41.30.114:9443", DataPowerGatewayTargets.hostLabel(targets.get(0).tokenUrl()));
        assertEquals("10.0.20.68:9443", DataPowerGatewayTargets.hostLabel(targets.get(1).sendUrl()));
        assertTrue(DataPowerGatewayTargets.shouldTryNextHost(503));
    }

    @Test
    void incompleteFallbackIsIgnored() {
        NotificationProperties.DataPower config = new NotificationProperties.DataPower();
        config.setTokenUrl("https://10.41.30.114:9443/oauth2/token");
        config.setSendUrl("https://10.41.30.114:9443/SMS/send");
        config.setFallbackTokenUrl("https://10.0.20.68:9443/oauth2/token");
        assertEquals(1, DataPowerGatewayTargets.of(config).size());
    }
}
