package com.notifyhub.support;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A tiny HTTP client for tests. Plain JDK, so it works with any Spring Boot version. */
public class TestApi {

    public record Response(int status, String body, HttpResponse<String> raw) {

        public String header(String name) {
            return raw.headers().firstValue(name).orElse(null);
        }

        /** The first "id" field of a flat JSON response. */
        public String id() {
            Matcher m = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"").matcher(body);
            if (!m.find()) {
                throw new IllegalStateException("No id in response: " + body);
            }
            return m.group(1);
        }
    }

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final String base;

    public TestApi(String base) {
        this.base = base;
    }

    public Response get(String path, Map<String, String> headers) {
        return send(HttpRequest.newBuilder(URI.create(base + path)).GET(), headers);
    }

    public Response post(String path, String json, Map<String, String> headers) {
        return send(HttpRequest.newBuilder(URI.create(base + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json)), headers);
    }

    private Response send(HttpRequest.Builder builder, Map<String, String> headers) {
        headers.forEach(builder::header);
        try {
            HttpResponse<String> response = client.send(
                    builder.timeout(Duration.ofSeconds(20)).build(), HttpResponse.BodyHandlers.ofString());
            return new Response(response.statusCode(), response.body(), response);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}