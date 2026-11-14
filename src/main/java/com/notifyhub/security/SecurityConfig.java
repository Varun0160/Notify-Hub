package com.notifyhub.security;

import com.notifyhub.ratelimit.LoginRateLimitFilter;
import com.notifyhub.ratelimit.RateLimitFilter;
import com.notifyhub.ratelimit.RateLimitProperties;
import com.notifyhub.ratelimit.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import java.io.IOException;
import java.time.Instant;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ApiKeyAuthenticator apiKeyAuthenticator;
    private final JwtDecoder jwtDecoder;

    private final RateLimiter rateLimiter;
    private final RateLimitProperties rateLimitProperties;

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // CHAIN 1: only for the documentation paths. Looser CSP so Swagger UI can load its scripts and styles.
    @Bean
    @Order(1)
    SecurityFilterChain docsFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs", "/v3/api-docs/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'self'; style-src 'self' 'unsafe-inline'; "
                                + "script-src 'self' 'unsafe-inline'; img-src 'self' data:; "
                                + "frame-ancestors 'none'")))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    // CHAIN 2: everything else (the real API). No securityMatcher, so it matches all remaining
    // requests, which is why it must come LAST (@Order(2)).
    @Bean
    @Order(2)
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                        .referrerPolicy(ref -> ref.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/ping", "/error").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh",
                                "/api/v1/auth/logout").permitAll()
                        .requestMatchers("/api/v1/webhooks/**").permitAll()
                        .requestMatchers("/api/v1/auth/me").hasAnyRole("ADMIN", "VIEWER")
                        .requestMatchers("/api/v1/recipients/**").hasRole("TENANT")
                        .requestMatchers("/api/v1/templates/**").hasRole("TENANT")
                        .requestMatchers("/api/v1/notifications/**").hasRole("TENANT")
                        .requestMatchers("/api/v1/tenants/**", "/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/me").hasRole("TENANT")
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) ->
                                writeError(request, response, HttpStatus.UNAUTHORIZED,
                                        "Missing or invalid credentials"))
                        .accessDeniedHandler((request, response, e) ->
                                writeError(request, response, HttpStatus.FORBIDDEN,
                                        "You do not have permission to access this resource")))
                .addFilterBefore(new ApiKeyAuthFilter(apiKeyAuthenticator),
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new JwtAuthFilter(jwtDecoder),
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new RateLimitFilter(rateLimiter, rateLimitProperties),
                        JwtAuthFilter.class)
                .addFilterAfter(new LoginRateLimitFilter(rateLimiter),
                        RateLimitFilter.class);
        return http.build();
    }

    private void writeError(HttpServletRequest request, HttpServletResponse response,
                            HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String json = """
                {"timestamp":"%s","status":%d,"error":"%s","message":"%s","path":"%s"}
                """.formatted(Instant.now(), status.value(),
                status.getReasonPhrase(), message, request.getRequestURI());
        response.getWriter().write(json);
    }
}