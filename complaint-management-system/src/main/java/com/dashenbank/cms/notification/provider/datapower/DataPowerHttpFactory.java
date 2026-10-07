package com.dashenbank.cms.notification.provider.datapower;

import com.dashenbank.cms.notification.NotificationProperties;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;

/**
 * Builds a JDK {@link HttpClient} with standard TLS verification. A custom
 * truststore is used only when {@code SMS_DATAPOWER_TRUSTSTORE_PATH} is set.
 * Trust-all and hostname-verifier bypasses are not supported.
 */
public final class DataPowerHttpFactory {

    private DataPowerHttpFactory() {
    }

    public static RestClient createRestClient(NotificationProperties.DataPower config) {
        HttpClient.Builder http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(1, config.getConnectTimeoutMs())))
                .followRedirects(HttpClient.Redirect.NEVER)
                .version(HttpClient.Version.HTTP_1_1);
        SSLContext sslContext = sslContext(config);
        if (sslContext != null) {
            http.sslContext(sslContext);
        }
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(http.build());
        requestFactory.setReadTimeout(Duration.ofMillis(Math.max(1, config.getReadTimeoutMs())));
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    public static void validateTruststore(NotificationProperties.DataPower config) {
        sslContext(config);
    }

    static SSLContext sslContext(NotificationProperties.DataPower config) {
        if (config == null || !StringUtils.hasText(config.getTruststorePath())) {
            return null;
        }
        Path path = Path.of(config.getTruststorePath().trim());
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException(
                    "SMS_DATAPOWER_TRUSTSTORE_PATH does not exist or is not a file");
        }
        char[] password = passwordChars(config.getTruststorePassword());
        try (InputStream in = Files.newInputStream(path)) {
            KeyStore store = KeyStore.getInstance(storeType(path));
            store.load(in, password);
            TrustManagerFactory trustManagers = TrustManagerFactory
                    .getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagers.init(store);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagers.getTrustManagers(), null);
            return sslContext;
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalStateException("SMS_DATAPOWER_TRUSTSTORE_PATH could not be loaded", e);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private static char[] passwordChars(String password) {
        return password == null ? new char[0] : password.toCharArray();
    }

    private static String storeType(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".p12") || name.endsWith(".pfx")) {
            return "PKCS12";
        }
        if (name.endsWith(".jks") || name.endsWith(".keystore")) {
            return "JKS";
        }
        return "PKCS12";
    }
}
