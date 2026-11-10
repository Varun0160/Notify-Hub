package com.notifyhub.provider;

import com.notifyhub.common.enums.Channel;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SendGridEmailProviderTest {

    private HttpServer server;
    private final AtomicReference<String> lastAuth = new AtomicReference<>();
    private final AtomicReference<String> lastBody = new AtomicReference<>();

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void startServer(int status, String messageId, long delayMs) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v3/mail/send", exchange -> {
            lastAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            if (delayMs > 0) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
            if (messageId != null) {
                exchange.getResponseHeaders().add("X-Message-Id", messageId);
            }
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        server.start();
    }

    private SendGridEmailProvider providerFor(int port) {
        return new SendGridEmailProvider("http://127.0.0.1:" + port, "SG.test-key", "from@example.com",
                Duration.ofMillis(500), Duration.ofMillis(300), JsonMapper.builder().build());
    }

    private SendGridEmailProvider provider() {
        return providerFor(server.getAddress().getPort());
    }

    private OutboundMessage message() {
        return new OutboundMessage(Channel.EMAIL, "ravi@example.com", "Welcome", "<p>Hi</p>");
    }

    @Test
    void acceptedRequestIsSuccessAndCarriesProviderId() throws IOException {
        startServer(202, "sg-123", 0);

        SendOutcome outcome = provider().send(message());

        assertEquals(SendOutcome.Status.SUCCESS, outcome.status());
        assertEquals("sg-123", outcome.providerMessageId());
        assertEquals("Bearer SG.test-key", lastAuth.get());
        assertTrue(lastBody.get().contains("ravi@example.com"));
        assertTrue(lastBody.get().contains("Welcome"));
    }

    @Test
    void badRequestIsPermanent() throws IOException {
        startServer(400, null, 0);
        assertEquals(SendOutcome.Status.PERMANENT_FAILURE, provider().send(message()).status());
    }

    @Test
    void rateLimitIsRetryable() throws IOException {
        startServer(429, null, 0);
        assertEquals(SendOutcome.Status.RETRYABLE_FAILURE, provider().send(message()).status());
    }

    @Test
    void serverErrorIsRetryable() throws IOException {
        startServer(503, null, 0);
        assertEquals(SendOutcome.Status.RETRYABLE_FAILURE, provider().send(message()).status());
    }

    @Test
    void slowServerTimesOutAndIsRetryable() throws IOException {
        startServer(202, "sg-slow", 1500);   // read timeout is 300 ms
        long started = System.currentTimeMillis();

        SendOutcome outcome = provider().send(message());

        assertEquals(SendOutcome.Status.RETRYABLE_FAILURE, outcome.status());
        assertTrue(System.currentTimeMillis() - started < 1400, "should give up well before the server answers");
    }

    @Test
    void unreachableServerIsRetryable() throws IOException {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }   // closed again: nothing is listening on it
        assertEquals(SendOutcome.Status.RETRYABLE_FAILURE, providerFor(closedPort).send(message()).status());
    }
}