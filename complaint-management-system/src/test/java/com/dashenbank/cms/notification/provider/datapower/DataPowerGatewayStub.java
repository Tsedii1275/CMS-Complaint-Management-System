package com.dashenbank.cms.notification.provider.datapower;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

final class DataPowerGatewayStub implements AutoCloseable {

    private final HttpServer server;
    private final java.util.concurrent.ExecutorService executor;
    final AtomicInteger tokenCalls = new AtomicInteger();
    final AtomicInteger sendCalls = new AtomicInteger();
    volatile int tokenStatus = 200;
    volatile int sendStatus = 200;
    volatile int tokenSleepMs;
    volatile int sendSleepMs;
    volatile String tokenBody = "{\"access_token\":\"tok-1\",\"expires_in\":3600,\"token_type\":\"Bearer\"}";
    volatile String sendBody = "{\"messageId\":\"sms-99\"}";
    final List<Integer> sendStatusSequence = new CopyOnWriteArrayList<>();
    final AtomicReference<String> lastSendJson = new AtomicReference<>();
    final AtomicReference<String> lastTokenAuthorization = new AtomicReference<>();
    final AtomicReference<String> lastSendAuthorization = new AtomicReference<>();
    final AtomicReference<String> lastTokenForm = new AtomicReference<>();
    final List<String> sendAuthorizationHistory = new ArrayList<>();

    DataPowerGatewayStub() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/oauth2/token", this::handleToken);
        server.createContext("/SMS/send", this::handleSend);
        executor = Executors.newCachedThreadPool();
        server.setExecutor(executor);
        server.start();
    }

    String tokenUrl() {
        return baseUrl() + "/oauth2/token";
    }

    String sendUrl() {
        return baseUrl() + "/SMS/send";
    }

    int port() {
        return server.getAddress().getPort();
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + port();
    }

    private void handleToken(HttpExchange exchange) throws IOException {
        tokenCalls.incrementAndGet();
        lastTokenAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
        lastTokenForm.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        sleepQuietly(tokenSleepMs);
        write(exchange, tokenStatus, tokenBody);
    }

    private void handleSend(HttpExchange exchange) throws IOException {
        int call = sendCalls.getAndIncrement();
        lastSendAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
        synchronized (sendAuthorizationHistory) {
            sendAuthorizationHistory.add(exchange.getRequestHeaders().getFirst("Authorization"));
        }
        lastSendJson.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        sleepQuietly(sendSleepMs);
        int status = sendStatus;
        if (call < sendStatusSequence.size() && sendStatusSequence.get(call) != null) {
            status = sendStatusSequence.get(call);
        }
        write(exchange, status, sendBody);
    }

    private static void write(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static void sleepQuietly(int millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }
}
