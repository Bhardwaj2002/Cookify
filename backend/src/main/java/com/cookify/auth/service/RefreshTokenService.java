package com.cookify.auth.service;

import com.cookify.auth.entity.RefreshToken;
import com.cookify.auth.entity.RevokeReason;
import com.cookify.auth.repository.RefreshTokenRepository;
import com.cookify.common.exception.UnauthorizedException;
import com.cookify.common.util.SecureTokens;
import com.cookify.security.jwt.JwtProperties;
import com.cookify.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Refresh tokens with rotation: every refresh revokes the presented token and issues a new one.
 * Presenting an already-revoked token means it was stolen or replayed, so all of that user's
 * sessions are revoked (reuse detection). Exception: a token rotated within the last few seconds
 * ({@code app.jwt.refresh-reuse-grace}) is treated as a concurrent refresh from another tab.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    /** Result of a successful rotation: the owner and the new raw token for the cookie. */
    public record Rotation(User user, String newToken) {
    }

    /** Creates a session and returns the raw token (only ever sent to the client, never stored). */
    @Transactional
    public String issue(User user, String userAgent) {
        String raw = SecureTokens.generate();
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(SecureTokens.sha256(raw));
        token.setUserAgent(truncate(userAgent));
        token.setExpiresAt(clock.instant().plus(jwtProperties.refreshTokenTtl()));
        repository.save(token);
        return raw;
    }

    // noRollbackFor: the reuse-detection revocation must be committed even though we then reject the request.
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public Rotation rotate(String rawToken, String userAgent) {
        Instant now = clock.instant();
        RefreshToken current = find(rawToken);

        if (current.isRevoked() && isConcurrentRefresh(current, now)) {
            User user = current.getUser();
            return new Rotation(user, issue(user, userAgent));
        }
        if (current.isRevoked()) {
            Long userId = current.getUser().getId();
            log.warn("Refresh token reuse detected for user {}; revoking all sessions", userId);
            repository.revokeAllForUser(userId, now);
            throw invalid();
        }
        if (current.isExpiredAt(now)) {
            throw invalid();
        }

        current.revoke(RevokeReason.ROTATED, now);
        User user = current.getUser();
        return new Rotation(user, issue(user, userAgent));
    }

    /** Logout: revokes the given session. Unknown or already-revoked tokens are ignored. */
    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        repository.findByTokenHash(SecureTokens.sha256(rawToken))
                .filter(t -> !t.isRevoked())
                .ifPresent(t -> t.revoke(RevokeReason.LOGOUT, clock.instant()));
    }

    @Transactional
    public void revokeAll(Long userId) {
        repository.revokeAllForUser(userId, clock.instant());
    }

    /** Rotated (not logged out / mass-revoked) moments ago — e.g. two tabs refreshing at once. */
    private boolean isConcurrentRefresh(RefreshToken token, Instant now) {
        return token.getRevokeReason() == RevokeReason.ROTATED
                && token.getRevokedAt().plus(jwtProperties.refreshReuseGrace()).isAfter(now);
    }

    private RefreshToken find(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw invalid();
        }
        return repository.findByTokenHash(SecureTokens.sha256(rawToken)).orElseThrow(this::invalid);
    }

    private UnauthorizedException invalid() {
        return new UnauthorizedException("INVALID_REFRESH_TOKEN", "Session expired, please log in again");
    }

    private static String truncate(String s) {
        return s == null || s.length() <= 255 ? s : s.substring(0, 255);
    }
}
