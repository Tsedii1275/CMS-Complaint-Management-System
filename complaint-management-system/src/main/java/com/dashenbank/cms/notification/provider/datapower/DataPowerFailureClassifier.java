package com.dashenbank.cms.notification.provider.datapower;

import com.dashenbank.cms.notification.provider.ProviderResult;
import org.springframework.web.client.ResourceAccessException;

import javax.net.ssl.SSLHandshakeException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;
import java.security.cert.CertificateException;

final class DataPowerFailureClassifier {

    private DataPowerFailureClassifier() {
    }

    static boolean retryableStatus(int status) {
        return status == 429 || status == 500 || status == 502 || status == 503 || status == 504;
    }

    static ProviderResult fromHttp(int status, String operation) {
        return fromHttp(status, operation, null);
    }

    static ProviderResult fromHttp(int status, String operation, String responseBody) {
        boolean destinationError = looksLikeDestinationOrNetworkError(responseBody);
        if (status >= 200 && status < 300) {
            return ProviderResult.delivered(null);
        }
        if (status == 401) {
            return ProviderResult.permanent(operation + " authentication failed after token refresh");
        }
        if (status == 400) {
            return ProviderResult.permanent(destinationError
                    ? operation + " destination/network rejected (HTTP 400)"
                    : operation + " invalid payload (HTTP 400)");
        }
        if (status == 403 || status == 404) {
            return ProviderResult.permanent(destinationError
                    ? operation + " destination/network rejected (HTTP " + status + ")"
                    : operation + " HTTP " + status);
        }
        if (retryableStatus(status)) {
            return ProviderResult.retryable(destinationError
                    ? operation + " destination/network error (HTTP " + status + ")"
                    : operation + " HTTP " + status);
        }
        if (status >= 400 && status < 500) {
            return ProviderResult.permanent(destinationError
                    ? operation + " destination/network rejected (HTTP " + status + ")"
                    : operation + " HTTP " + status);
        }
        return ProviderResult.retryable(destinationError
                ? operation + " destination/network error (HTTP " + status + ")"
                : operation + " HTTP " + status);
    }

    static boolean looksLikeDestinationOrNetworkError(String body) {
        if (body == null || body.isBlank()) {
            return false;
        }
        String lower = body.toLowerCase();
        return lower.contains("destination")
                || lower.contains("invalid number")
                || lower.contains("invalid phone")
                || lower.contains("invalid msisdn")
                || lower.contains("unsupported network")
                || lower.contains("unknown network")
                || lower.contains("operator")
                || lower.contains("safaricom")
                || lower.contains("ethio");
    }

    static ProviderResult fromException(String operation, Throwable error) {
        Throwable root = root(error);
        if (isTimeout(root) || isTemporaryConnection(root)) {
            return ProviderResult.retryable(operation + " " + root.getClass().getSimpleName());
        }
        if (isTlsFailure(root)) {
            return ProviderResult.permanent(operation + " TLS verification failed");
        }
        if (error instanceof ResourceAccessException && isTimeout(root(error.getCause()))) {
            return ProviderResult.retryable(operation + " timeout");
        }
        if (error instanceof ResourceAccessException) {
            Throwable cause = root(error.getCause());
            if (isTemporaryConnection(cause)) {
                return ProviderResult.retryable(operation + " " + cause.getClass().getSimpleName());
            }
            if (isTlsFailure(cause)) {
                return ProviderResult.permanent(operation + " TLS verification failed");
            }
            return ProviderResult.retryable(operation + " connection failure");
        }
        return ProviderResult.retryable(operation + " " + root.getClass().getSimpleName());
    }

    private static boolean isTimeout(Throwable error) {
        return error instanceof SocketTimeoutException
                || error instanceof HttpTimeoutException
                || error instanceof HttpConnectTimeoutException;
    }

    private static boolean isTemporaryConnection(Throwable error) {
        return error instanceof ConnectException || error instanceof UnknownHostException;
    }

    private static boolean isTlsFailure(Throwable error) {
        return error instanceof SSLHandshakeException || error instanceof CertificateException;
    }

    private static Throwable root(Throwable error) {
        Throwable current = error;
        while (current != null && current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current == null ? error : current;
    }
}
