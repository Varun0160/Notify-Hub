package com.notifyhub.provider;

import com.notifyhub.common.enums.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
public class SendGridEmailProvider implements NotificationProvider {

    private final RestClient client;
    private final ObjectMapper objectMapper;
    private final String fromEmail;

    public SendGridEmailProvider(String baseUrl, String apiKey, String fromEmail,
                                 Duration connectTimeout, Duration readTimeout,
                                 ObjectMapper objectMapper) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(connectTimeout)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(readTimeout);

        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
        this.objectMapper = objectMapper;
        this.fromEmail = fromEmail;
    }

    @Override
    public Channel channel() {
        return Channel.EMAIL;
    }

    @Override
    public String name() {
        return "sendgrid";
    }

    @Override
    public SendOutcome send(OutboundMessage message) {
        try {
            Map<String, Object> payload = Map.of(
                    "personalizations", List.of(Map.of("to", List.of(Map.of("email", message.to())))),
                    "from", Map.of("email", fromEmail),
                    "subject", message.subject(),
                    "content", List.of(Map.of("type", "text/html", "value", message.body())));

            ResponseEntity<Void> response = client.post()
                    .uri("/v3/mail/send")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(payload))
                    .retrieve()
                    .toBodilessEntity();

            return SendOutcome.success(response.getHeaders().getFirst("X-Message-Id"));

        } catch (RestClientResponseException ex) {
            // The provider answered with an error status (4xx or 5xx).
            HttpStatusCode status = ex.getStatusCode();
            String error = "SendGrid HTTP " + status.value();
            if (status.value() == 429 || status.value() == 408 || status.is5xxServerError()) {
                return SendOutcome.retryable(error);
            }
            if (status.value() == 401) {
                log.error("SendGrid rejected our API key (401). Check SENDGRID_API_KEY.");
            }
            return SendOutcome.permanent(error);

        } catch (ResourceAccessException ex) {
            // No answer at all: timeout, connection refused, DNS failure.
            return SendOutcome.retryable("SendGrid unreachable: " + ex.getClass().getSimpleName());

        } catch (RuntimeException ex) {
            // Anything unexpected: safest to treat as retryable (the retry cap prevents loops).
            log.warn("Unexpected error calling SendGrid", ex);
            return SendOutcome.retryable("Unexpected error: " + ex.getClass().getSimpleName());
        }
    }
}