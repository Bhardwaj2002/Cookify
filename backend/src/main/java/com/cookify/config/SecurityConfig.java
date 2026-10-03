package com.cookify.config;

import com.cookify.security.BanCheckFilter;
import com.cookify.security.JsonSecurityErrorHandler;
import com.cookify.security.jwt.AuthUserJwtConverter;
import com.cookify.security.ratelimit.RateLimitFilter;
import com.cookify.security.ratelimit.RateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import com.cookify.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import java.time.Clock;

/**
 * HTTP security rules. The API is stateless: each request carries a JWT access token
 * ({@code Authorization: Bearer ...}), validated by Spring's OAuth2 resource-server support.
 * Sessions, CSRF tokens, form login and HTTP basic are therefore disabled.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    /** Endpoints reachable without authentication. */
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/health",
            "/api/auth/**",
            "/media/**",
            "/ws", "/ws/**", // WebSocket handshake; STOMP CONNECT is authenticated by StompAuthInterceptor
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    private final JsonSecurityErrorHandler securityErrorHandler;
    private final AuthUserJwtConverter authUserJwtConverter;
    private final UserRepository userRepository;
    private final RateLimiter rateLimiter;
    private final Clock clock;

    @Value("${app.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        if (rateLimitEnabled) {
            // After authentication (so per-user rules know the caller) and after the ban check.
            http.addFilterAfter(new RateLimitFilter(rateLimiter, securityErrorHandler), BearerTokenAuthenticationFilter.class);
        }
        return http
                .headers(headers -> headers
                        // Defaults already send X-Content-Type-Options, X-Frame-Options: DENY, Cache-Control
                        // and HSTS (on HTTPS). Add a strict referrer policy and turn off unused browser features.
                        .referrerPolicy(rp -> rp.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        // (The frontend's Content-Security-Policy is set where the pages are served — CloudFront.)
                        .permissionsPolicyHeader(pp -> pp.policy("camera=(), microphone=(), geolocation=(), payment=()")))
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(rs -> rs
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(authUserJwtConverter))
                        .authenticationEntryPoint(securityErrorHandler)
                        .accessDeniedHandler(securityErrorHandler))
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(securityErrorHandler)
                        .accessDeniedHandler(securityErrorHandler))
                // Banned users are stopped even while their access token is still valid.
                .addFilterAfter(new BanCheckFilter(userRepository, securityErrorHandler, clock),
                        BearerTokenAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Order matters: the caller's own account first, then public profiles.
                        .requestMatchers("/api/users/me", "/api/users/me/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/users/*", "/api/users/*/recipes").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/recipes", "/api/recipes/**", "/api/meta/**").permitAll()
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())
                .build();
    }
}
