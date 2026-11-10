package com.notifyhub.ratelimit;

import com.notifyhub.security.TenantPrincipal;
import com.notifyhub.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitFilterTest {

    private final RateLimitProperties props = new RateLimitProperties(true,
            new RateLimitProperties.Limit(20, 5), new RateLimitProperties.Limit(3, 0.0166));

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(Object principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private MockHttpServletResponse run(RateLimiter limiter, String uri, MockFilterChain chain) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        new RateLimitFilter(limiter, props)
                .doFilter(new MockHttpServletRequest("GET", uri), response, chain);
        return response;
    }

    @Test
    void allowedRequestPassesAndCarriesRateLimitHeaders() throws Exception {
        authenticateAs(new TenantPrincipal(7L, "t-7", "demo"));
        MockFilterChain chain = new MockFilterChain();

        MockHttpServletResponse response =
                run((key, cap, refill) -> new RateLimitDecision(true, 7, 0), "/api/v1/me", chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest(), "the request must continue down the chain");
        assertEquals("20", response.getHeader("X-RateLimit-Limit"));
        assertEquals("7", response.getHeader("X-RateLimit-Remaining"));
    }

    @Test
    void rejectedRequestGets429WithRetryAfterAndStopsTheChain() throws Exception {
        authenticateAs(new TenantPrincipal(7L, "t-7", "demo"));
        MockFilterChain chain = new MockFilterChain();

        MockHttpServletResponse response =
                run((key, cap, refill) -> new RateLimitDecision(false, 0, 3200), "/api/v1/me", chain);

        assertEquals(429, response.getStatus());
        assertEquals("4", response.getHeader("Retry-After"));       // 3.2 s rounded up
        assertTrue(response.getContentAsString().contains("Rate limit exceeded"));
        assertNull(chain.getRequest(), "a rejected request must never reach the controller");
    }

    @Test
    void bucketKeyIsPerTenant() throws Exception {
        AtomicReference<String> seenKey = new AtomicReference<>();
        authenticateAs(new TenantPrincipal(42L, "t-42", "demo"));

        run((key, cap, refill) -> {
            seenKey.set(key);
            return new RateLimitDecision(true, 1, 0);
        }, "/api/v1/notifications", new MockFilterChain());

        assertEquals("notifyhub:rl:tenant:42", seenKey.get());
    }

    @Test
    void anonymousRequestsAreNotLimitedHere() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        MockFilterChain chain = new MockFilterChain();

        run((key, cap, refill) -> {
            calls.incrementAndGet();
            return new RateLimitDecision(false, 0, 1000);
        }, "/api/v1/ping", chain);

        assertEquals(0, calls.get());
        assertNotNull(chain.getRequest());
    }

    @Test
    void dashboardUsersAreNotLimitedByTheTenantBucket() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        authenticateAs(new UserPrincipal("u-1", "admin@x.com", "ADMIN"));

        run((key, cap, refill) -> {
            calls.incrementAndGet();
            return new RateLimitDecision(false, 0, 1000);
        }, "/api/v1/tenants", new MockFilterChain());

        assertEquals(0, calls.get());
    }

    @Test
    void nonApiPathsAreNotLimited() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        authenticateAs(new TenantPrincipal(7L, "t-7", "demo"));

        run((key, cap, refill) -> {
            calls.incrementAndGet();
            return new RateLimitDecision(false, 0, 1000);
        }, "/actuator/health", new MockFilterChain());

        assertEquals(0, calls.get());
    }
}