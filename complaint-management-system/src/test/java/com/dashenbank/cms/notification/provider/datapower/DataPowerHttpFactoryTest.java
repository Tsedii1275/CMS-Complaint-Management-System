package com.dashenbank.cms.notification.provider.datapower;

import com.dashenbank.cms.notification.NotificationProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.net.ssl.SSLContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataPowerHttpFactoryTest {

    @Test
    void usesJvmDefaultTrustWhenTruststorePathBlank() {
        NotificationProperties.DataPower config = new NotificationProperties.DataPower();
        config.setTruststorePath(" ");
        assertNull(DataPowerHttpFactory.sslContext(config));
    }

    @Test
    void missingTruststoreFailsFast() {
        NotificationProperties.DataPower config = new NotificationProperties.DataPower();
        config.setTruststorePath("D:/does-not-exist-sms-trust.p12");
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> DataPowerHttpFactory.validateTruststore(config));
        assertTrue(error.getMessage().contains("SMS_DATAPOWER_TRUSTSTORE_PATH"));
    }

    @Test
    void loadsConfiguredPkcs12Truststore(@TempDir Path dir) throws Exception {
        Path store = dir.resolve("dashen-ca.p12");
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, "changeit".toCharArray());
        try (var out = Files.newOutputStream(store)) {
            ks.store(out, "changeit".toCharArray());
        }
        NotificationProperties.DataPower config = new NotificationProperties.DataPower();
        config.setTruststorePath(store.toString());
        config.setTruststorePassword("changeit");
        SSLContext ssl = DataPowerHttpFactory.sslContext(config);
        assertNotNull(ssl);
    }
}
