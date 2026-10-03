package com.cookify.security.ratelimit;

import com.cookify.security.JsonSecurityErrorHandler;
import com.cookify.security.jwt.AuthUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * Throttles abuse-prone endpoints: brute-forcing logins / 2FA codes, mass sign-ups, reset-email
 * spam (per client IP) and comment / chat / rating flooding (per logged-in user).
 * Over the limit → {@code 429 TOO_MANY_REQUESTS} with a {@code Retry-After} header.
 * Created by SecurityConfig (runs after authentication, so per-user rules know the caller).
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private enum Scope { IP, USER }

    private record Rule(String method, String pattern, int limit, Duration window, Scope scope) {
    }

    private static final List<Rule> RULES = List.of(
            new Rule("POST", "/api/auth/login", 10, Duration.ofMinutes(1), Scope.IP),
            new Rule("POST", "/api/auth/verify-otp", 10, Duration.ofMinutes(1), Scope.IP),
            new Rule("POST", "/api/auth/signup", 5, Duration.ofMinutes(10), Scope.IP),
            new Rule("POST", "/api/auth/forgot-password", 5, Duration.ofMinutes(15), Scope.IP),
            new Rule("POST", "/api/auth/reset-password", 10, Duration.ofMinutes(15), Scope.IP),
            new Rule("POST", "/api/auth/refresh", 60, Duration.ofMinutes(1), Scope.IP),
            new Rule("POST", "/api/recipes/*/comments", 20, Duration.ofMinutes(1), Scope.USER),
            new Rule("POST", "/api/chat/conversations/*/messages", 60, Duration.ofMinutes(1), Scope.USER),
            new Rule("PUT", "/api/recipes/*/rating", 30, Duration.ofMinutes(1), Scope.USER),
            new Rule("POST", "/api/recipes", 10, Duration.ofMinutes(10), Scope.USER),
            new Rule("POST", "/api/recipes/*/media", 60, Duration.ofMinutes(10), Scope.USER));

    private final AntPathMatcher matcher = new AntPathMatcher();
    private final RateLimiter limiter;
    private final JsonSecurityErrorHandler errorWriter;

    public RateLimitFilter(RateLimiter limiter, JsonSecurityErrorHandler errorWriter) {
        this.limiter = limiter;
        this.errorWriter = errorWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        for (Rule rule : RULES) {
            if (!rule.method().equals(request.getMethod()) || !matcher.match(rule.pattern(), path)) {
                continue;
            }
            String who = rule.scope() == Scope.USER ? userKey(request) : "ip:" + request.getRemoteAddr();
            long retryAfter = limiter.tryAcquire(rule.pattern() + "|" + who, rule.limit(), rule.window());
            if (retryAfter > 0) {
                response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter));
                errorWriter.write(response, HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS",
                        "Too many attempts. Please wait " + retryAfter + " seconds and try again.", request);
                return;
            }
            break; // one rule per request
        }
        chain.doFilter(request, response);
    }

    /** Per-user for logged-in callers; falls back to IP (the request will be rejected as 401 anyway). */
    private static String userKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthUser user) {
            return "user:" + user.id();
        }
        return "ip:" + request.getRemoteAddr();
    }
}
