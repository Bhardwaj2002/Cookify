package com.cookify.security;

import com.cookify.security.jwt.AuthUser;
import com.cookify.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;

/**
 * Rejects requests from banned users even while their (short-lived) access token is still valid.
 * Runs right after JWT authentication; created by SecurityConfig (not a bean, so it only runs in
 * the security chain).
 */
public class BanCheckFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final JsonSecurityErrorHandler errorWriter;
    private final Clock clock;

    public BanCheckFilter(UserRepository userRepository, JsonSecurityErrorHandler errorWriter, Clock clock) {
        this.userRepository = userRepository;
        this.errorWriter = errorWriter;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthUser user
                && userRepository.isBanned(user.id(), clock.instant())) {
            SecurityContextHolder.clearContext();
            errorWriter.write(response, HttpStatus.FORBIDDEN, "ACCOUNT_BANNED", "This account has been suspended", request);
            return;
        }
        chain.doFilter(request, response);
    }
}
