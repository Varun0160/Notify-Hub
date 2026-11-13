package com.notifyhub.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginRateLimitFilterTest {

    private MockHttpServletResponse run(RateLimiter limiter, MockHttpServletRequest request, MockFilterChain chain)
            throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        new LoginRateLimitFilter(limiter).doFilter(request, response, chain);
        return response;
    }

    private MockHttpServletRequest post(String uri) {
        return new MockHttpServletRequest("POST", uri);
    }

    @Test
    void anAllowedLoginAttemptContinues() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        MockHttpServletResponse response = run((k, c, r) -> new RateLimitDecision(true, 4, 0),
                post("/api/v1/auth/login"), chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
    }

    @Test
    void aBlockedAttemptGets429WithRetryAfterAndNeverReachesThePasswordCheck() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        MockHttpServletResponse response = run((k, c, r) -> new RateLimitDecision(false, 0, 8200),
                post("/api/v1/auth/login"), chain);

        assertEquals(429, response.getStatus());
        assertEquals("9", response.getHeader("Retry-After"));
        assertTrue(response.getContentAsString().contains("Too many attempts"));
        assertNull(chain.getRequest());
    }

    @Test
    void theBucketIsPerClientAddress() throws Exception {
        AtomicReference<String> key = new AtomicReference<>();
        MockHttpServletRequest request = post("/api/v1/auth/refresh");
        request.setRemoteAddr("203.0.113.9");

        run((k, c, r) -> {
            key.set(k);
            return new RateLimitDecision(true, 1, 0);
        }, request, new MockFilterChain());

        assertEquals("notifyhub:rl:login:203.0.113.9", key.get());
    }

    @Test
    void otherPathsAndMethodsAreNotLimited() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        RateLimiter counting = (k, c, r) -> {
            calls.incrementAndGet();
            return new RateLimitDecision(false, 0, 1000);
        };

        run(counting, post("/api/v1/notifications"), new MockFilterChain());
        run(counting, post("/api/v1/auth/logout"), new MockFilterChain());
        run(counting, new MockHttpServletRequest("GET", "/api/v1/auth/login"), new MockFilterChain());

        assertEquals(0, calls.get());
    }
}