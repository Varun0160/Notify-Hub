package com.notifyhub.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;

/** Slows down password guessing: a token bucket per client IP on the credential endpoints. */
public class LoginRateLimitFilter extends OncePerRequestFilter {

    static final int CAPACITY = 5;                 // a burst of 5 attempts...
    static final double REFILL_PER_SECOND = 0.1;   // ...then 1 attempt every 10 seconds

    private static final Set<String> PROTECTED_PATHS = Set.of("/api/v1/auth/login", "/api/v1/auth/refresh");

    private final RateLimiter limiter;

    public LoginRateLimitFilter(RateLimiter limiter) {
        this.limiter = limiter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !PROTECTED_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // getRemoteAddr(): behind a proxy this is the proxy. Configure forwarded headers for a TRUSTED proxy only.
        RateLimitDecision decision = limiter.tryConsume(
                "notifyhub:rl:login:" + request.getRemoteAddr(), CAPACITY, REFILL_PER_SECOND);

        if (!decision.allowed()) {
            long retryAfter = decision.retryAfterSeconds();
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("""
                    {"timestamp":"%s","status":429,"error":"Too Many Requests","message":"Too many attempts. Retry after %d seconds","path":"%s"}
                    """.formatted(Instant.now(), retryAfter, request.getRequestURI()));
            return;                                    // never reaches the password check
        }
        chain.doFilter(request, response);
    }
}