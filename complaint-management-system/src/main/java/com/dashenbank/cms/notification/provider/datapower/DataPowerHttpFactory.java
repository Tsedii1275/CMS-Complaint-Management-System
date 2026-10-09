package com.dashenbank.cms.notification.provider.datapower;

import com.dashenbank.cms.notification.NotificationProperties;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Builds a JDK {@link HttpClient} with standard TLS verification. A custom
 * truststore is used only when {@code SMS_DATAPOWER_TRUSTSTORE_PATH} is set.
 * When URLs use an IP, {@code SMS_DATAPOWER_SSL_PEER_NAME} is verified on the
 * certificate (SNI + hostname) — not trust-all.
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
        if (StringUtils.hasText(peerName(config))) {
            SSLParameters params = new SSLParameters();
            params.setServerNames(List.of(new SNIHostName(peerName(config))));
            http.sslParameters(params);
        }
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(http.build());
        requestFactory.setReadTimeout(Duration.ofMillis(Math.max(1, config.getReadTimeoutMs())));
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    public static void validateTruststore(NotificationProperties.DataPower config) {
        sslContext(config);
    }

    static SSLContext sslContext(NotificationProperties.DataPower config) {
        if (config == null) {
            return null;
        }
        boolean customStore = StringUtils.hasText(config.getTruststorePath());
        boolean peer = StringUtils.hasText(peerName(config));
        if (!customStore && !peer) {
            return null;
        }
        try {
            X509TrustManager pkix = customStore ? loadCustomTrustManager(config) : defaultTrustManager();
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] { wrapping(pkix, peerName(config)) }, null);
            return sslContext;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SMS DataPower TLS context could not be initialized", e);
        }
    }

    private static X509TrustManager loadCustomTrustManager(NotificationProperties.DataPower config)
            throws GeneralSecurityException {
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
            return findX509(trustManagers.getTrustManagers());
        } catch (IOException e) {
            throw new IllegalStateException("SMS_DATAPOWER_TRUSTSTORE_PATH could not be loaded", e);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private static X509TrustManager defaultTrustManager() throws GeneralSecurityException {
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init((KeyStore) null);
        return findX509(factory.getTrustManagers());
    }

    private static X509TrustManager findX509(TrustManager[] managers) {
        for (TrustManager manager : managers) {
            if (manager instanceof X509TrustManager x509) {
                return x509;
            }
        }
        throw new IllegalStateException("No X509 trust manager in the JVM trust store");
    }

    private static X509ExtendedTrustManager wrapping(X509TrustManager pkix, String peerName) {
        return new X509ExtendedTrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket)
                    throws CertificateException {
                pkix.checkClientTrusted(chain, authType);
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket)
                    throws CertificateException {
                checkServer(chain, authType);
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
                    throws CertificateException {
                pkix.checkClientTrusted(chain, authType);
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
                    throws CertificateException {
                checkServer(chain, authType);
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                pkix.checkClientTrusted(chain, authType);
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                checkServer(chain, authType);
            }

            private void checkServer(X509Certificate[] chain, String authType) throws CertificateException {
                pkix.checkServerTrusted(chain, authType);
                if (StringUtils.hasText(peerName)) {
                    verifyPeer(chain, peerName);
                }
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return pkix.getAcceptedIssuers();
            }
        };
    }

    static void verifyPeer(X509Certificate[] chain, String peerName) throws CertificateException {
        if (chain == null || chain.length == 0 || !StringUtils.hasText(peerName)) {
            throw new CertificateException("DataPower TLS peer name is not configured");
        }
        if (!hostnameMatches(chain[0], peerName.trim())) {
            throw new CertificateException("DataPower certificate does not match " + peerName.trim());
        }
    }

    static boolean hostnameMatches(X509Certificate cert, String hostname) throws CertificateException {
        String expected = hostname.toLowerCase(Locale.ROOT);
        Collection<List<?>> sans = cert.getSubjectAlternativeNames();
        if (sans != null) {
            for (List<?> san : sans) {
                if (san.size() < 2 || !(san.get(0) instanceof Integer type)) {
                    continue;
                }
                if (type == 2 && san.get(1) instanceof String dns
                        && dnsNameMatches(dns.toLowerCase(Locale.ROOT), expected)) {
                    return true;
                }
            }
        }
        String cn = commonName(cert.getSubjectX500Principal().getName());
        return cn != null && dnsNameMatches(cn.toLowerCase(Locale.ROOT), expected);
    }

    private static boolean dnsNameMatches(String pattern, String hostname) {
        if (pattern.equals(hostname)) {
            return true;
        }
        if (pattern.startsWith("*.") && hostname.contains(".")) {
            return hostname.substring(hostname.indexOf('.') + 1).equals(pattern.substring(2));
        }
        return false;
    }

    private static String commonName(String distinguishedName) {
        for (String part : distinguishedName.split(",")) {
            String item = part.trim();
            if (item.regionMatches(true, 0, "CN=", 0, 3)) {
                return item.substring(3).trim();
            }
        }
        return null;
    }

    private static String peerName(NotificationProperties.DataPower config) {
        return config == null || !StringUtils.hasText(config.getSslPeerName())
                ? ""
                : config.getSslPeerName().trim();
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
