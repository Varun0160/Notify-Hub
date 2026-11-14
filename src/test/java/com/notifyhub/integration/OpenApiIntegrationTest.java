package com.notifyhub.integration;

import com.notifyhub.support.BaseIntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiIntegrationTest extends BaseIntegrationTest {

    @Test
    void theApiDescriptionIsPublishedAndKnowsOurEndpointsAndSecuritySchemes() {
        var response = api().get("/v3/api-docs", Map.of());

        assertEquals(200, response.status());
        assertTrue(response.body().contains("/api/v1/notifications"));
        assertTrue(response.body().contains("ApiKeyAuth"));
        assertTrue(response.body().contains("BearerAuth"));
    }

    @Test
    void swaggerUiIsServedWithAPolicyThatLetsItRun() {
        var response = api().get("/swagger-ui/index.html", Map.of());

        assertEquals(200, response.status());
        String csp = response.header("Content-Security-Policy");
        assertNotNull(csp);
        assertFalse(csp.contains("default-src 'none'"), "the strict API policy would blank the UI");
    }

    @Test
    void theApiItselfKeepsTheStrictPolicy() {
        var response = api().get("/api/v1/ping", Map.of());

        assertTrue(response.header("Content-Security-Policy").contains("default-src 'none'"));
    }

    @Test
    void docsDoNotWeakenAuthenticationOnRealEndpoints() {
        assertEquals(401, api().get("/api/v1/me", Map.of()).status());
    }
}