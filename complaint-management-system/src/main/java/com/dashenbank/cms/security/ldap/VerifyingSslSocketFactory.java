package com.dashenbank.cms.security.ldap;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Uses the JVM default trust store (no trust-all) and verifies the certificate
 * hostname against {@code cms.ldap.ssl.peer} when the LDAP URL is an IP.
 */
public final class VerifyingSslSocketFactory extends SSLSocketFactory {

    public static final String PEER_PROPERTY = "cms.ldap.ssl.peer";

    private final SSLSocketFactory delegate;
    private final String peerName;

    public VerifyingSslSocketFactory() {
        this.peerName = System.getProperty(PEER_PROPERTY, "");
        try {
            this.delegate = createFactory(peerName);
        } catch (Exception e) {
            throw new IllegalStateException("TLS context could not be initialized", e);
        }
    }

    static SSLSocketFactory createFactory(String peerName) throws Exception {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init((KeyStore) null);
        X509TrustManager pkix = findX509(tmf.getTrustManagers());
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, new TrustManager[] { wrapping(pkix, peerName) }, null);
        return ctx.getSocketFactory();
    }

    private static X509TrustManager findX509(TrustManager[] managers) {
        for (TrustManager manager : managers) {
            if (manager instanceof X509TrustManager x509) {
                return x509;
            }
        }
        throw new IllegalStateException("No X509 trust manager in the JVM default store");
    }

    private static X509ExtendedTrustManager wrapping(X509TrustManager pkix, String peerName) {
        return new X509ExtendedTrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket)
                    throws CertificateException {
                if (pkix instanceof X509ExtendedTrustManager ext) {
                    ext.checkClientTrusted(chain, authType, socket);
                } else {
                    pkix.checkClientTrusted(chain, authType);
                }
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket)
                    throws CertificateException {
                if (pkix instanceof X509ExtendedTrustManager ext) {
                    ext.checkServerTrusted(chain, authType, socket);
                } else {
                    pkix.checkServerTrusted(chain, authType);
                }
                verifyPeer(chain, peerName);
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
                    throws CertificateException {
                if (pkix instanceof X509ExtendedTrustManager ext) {
                    ext.checkClientTrusted(chain, authType, engine);
                } else {
                    pkix.checkClientTrusted(chain, authType);
                }
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
                    throws CertificateException {
                if (pkix instanceof X509ExtendedTrustManager ext) {
                    ext.checkServerTrusted(chain, authType, engine);
                } else {
                    pkix.checkServerTrusted(chain, authType);
                }
                verifyPeer(chain, peerName);
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                pkix.checkClientTrusted(chain, authType);
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                pkix.checkServerTrusted(chain, authType);
                verifyPeer(chain, peerName);
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return pkix.getAcceptedIssuers();
            }
        };
    }

    static void verifyPeer(X509Certificate[] chain, String peerName) throws CertificateException {
        if (chain == null || chain.length == 0 || peerName == null || peerName.isBlank()) {
            throw new CertificateException("LDAP TLS peer name is not configured");
        }
        if (!hostnameMatches(chain[0], peerName.trim())) {
            throw new CertificateException("LDAP certificate does not match " + peerName);
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

    private Socket apply(Socket socket) {
        if (socket instanceof SSLSocket ssl && peerName != null && !peerName.isBlank()) {
            SSLParameters params = ssl.getSSLParameters();
            params.setServerNames(List.of(new SNIHostName(peerName)));
            ssl.setSSLParameters(params);
        }
        return socket;
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return delegate.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return delegate.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return apply(delegate.createSocket(s, host, port, autoClose));
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return apply(delegate.createSocket(host, port));
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        return apply(delegate.createSocket(host, port, localHost, localPort));
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return apply(delegate.createSocket(host, port));
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort)
            throws IOException {
        return apply(delegate.createSocket(address, port, localAddress, localPort));
    }
}
