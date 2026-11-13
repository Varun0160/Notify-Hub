package com.notifyhub.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String id = CorrelationId.fromHeaderOrNew(request.getHeader(CorrelationId.HEADER));
        MDC.put(CorrelationId.MDC_KEY, id);
        response.setHeader(CorrelationId.HEADER, id);
        long started = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            String path = request.getRequestURI();      // path only: query strings may hold personal data
            if (!path.startsWith("/actuator")) {
                log.info("{} {} -> {} ({} ms)", request.getMethod(), path, response.getStatus(),
                        (System.nanoTime() - started) / 1_000_000);
            }
            MDC.remove(CorrelationId.MDC_KEY);          // threads are reused: never leak the id
        }
    }
}