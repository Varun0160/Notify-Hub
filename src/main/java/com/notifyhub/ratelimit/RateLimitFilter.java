package com.notifyhub.ratelimit;

import com.notifyhub.security.TenantPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter limiter;
    private final RateLimitProperties props;

    public RateLimitFilter(RateLimiter limiter, RateLimitProperties props) {
        this.limiter = limiter;
        this.props = props;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getPrincipal() instanceof TenantPrincipal tenant
                && request.getRequestURI().startsWith("/api/v1/")) {

            RateLimitProperties.Limit limit = props.tenant();
            RateLimitDecision decision = limiter.tryConsume(
                    "notifyhub:rl:tenant:" + tenant.tenantId(), limit.capacity(), limit.refillPerSecond());

            response.setHeader("X-RateLimit-Limit", String.valueOf(limit.capacity()));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));

            if (!decision.allowed()) {
                reject(request, response, decision.retryAfterSeconds());
                return;                                   // do NOT continue the chain
            }
        }
        chain.doFilter(request, response);
    }

    private void reject(HttpServletRequest request, HttpServletResponse response,
                        long retryAfterSeconds) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String json = """
                {"timestamp":"%s","status":429,"error":"Too Many Requests","message":"Rate limit exceeded. Retry after %d seconds","path":"%s"}
                """.formatted(Instant.now(), retryAfterSeconds, request.getRequestURI());
        response.getWriter().write(json);
    }
}