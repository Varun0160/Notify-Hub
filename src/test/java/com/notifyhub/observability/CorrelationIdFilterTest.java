package com.notifyhub.observability;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void cleanUp() {
        MDC.clear();
    }

    private MockHttpServletRequest request(String headerValue) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/ping");
        if (headerValue != null) {
            request.addHeader(CorrelationId.HEADER, headerValue);
        }
        return request;
    }

    @Test
    void anIncomingIdIsUsedDuringTheRequestAndEchoedBack() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> seen = new AtomicReference<>();

        filter.doFilter(request("demo-trace-001"), response, (req, res) -> seen.set(MDC.get(CorrelationId.MDC_KEY)));

        assertEquals("demo-trace-001", seen.get());
        assertEquals("demo-trace-001", response.getHeader(CorrelationId.HEADER));
    }

    @Test
    void aMissingIdIsGeneratedAndReturned() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> seen = new AtomicReference<>();

        filter.doFilter(request(null), response, (req, res) -> seen.set(MDC.get(CorrelationId.MDC_KEY)));

        assertEquals(36, seen.get().length());
        assertEquals(seen.get(), response.getHeader(CorrelationId.HEADER));
    }

    @Test
    void anUnsafeIdIsReplaced() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("bad id with spaces"), response, (req, res) -> { });

        assertNotEquals("bad id with spaces", response.getHeader(CorrelationId.HEADER));
    }

    @Test
    void theIdIsRemovedAfterTheRequestSoPooledThreadsDoNotLeakIt() throws Exception {
        filter.doFilter(request("demo-trace-001"), new MockHttpServletResponse(), (req, res) -> { });

        assertNull(MDC.get(CorrelationId.MDC_KEY));
    }

    @Test
    void theIdIsRemovedEvenWhenTheRequestFails() {
        assertThrows(ServletException.class, () ->
                filter.doFilter(request("demo-trace-001"), new MockHttpServletResponse(),
                        (req, res) -> { throw new ServletException("boom"); }));

        assertNull(MDC.get(CorrelationId.MDC_KEY));
    }
}