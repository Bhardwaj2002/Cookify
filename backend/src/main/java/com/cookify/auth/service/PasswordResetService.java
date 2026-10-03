package com.cookify.auth.service;

import com.cookify.auth.entity.PasswordResetToken;
import com.cookify.auth.repository.PasswordResetTokenRepository;
import com.cookify.common.exception.BadRequestException;
import com.cookify.common.util.SecureTokens;
import com.cookify.notification.EmailTemplate;
import com.cookify.notification.Notifier;
import com.cookify.security.device.TrustedDeviceService;
import com.cookify.user.entity.User;
import com.cookify.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

/** "Forgot password?" flow: email a one-time link, then set a new password with its token. */
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final RefreshTokenService refreshTokenService;
    private final TrustedDeviceService trustedDeviceService;
    private final PasswordEncoder passwordEncoder;
    private final Notifier notifier;
    private final PasswordResetProperties props;
    private final Clock clock;

    /**
     * Sends a reset link if the identifier matches an account. Always completes silently so the
     * response never reveals whether an account exists.
     */
    @Transactional
    public void requestReset(String identifier) {
        userRepository.findByLoginIdentifier(identifier.trim()).ifPresent(user -> {
            Instant now = clock.instant();
            tokenRepository.invalidatePending(user.getId(), now);

            String raw = SecureTokens.generate();
            PasswordResetToken token = new PasswordResetToken();
            token.setUser(user);
            token.setTokenHash(SecureTokens.sha256(raw));
            token.setExpiresAt(now.plus(props.tokenTtl()));
            tokenRepository.save(token);

            String link = UriComponentsBuilder.fromUriString(props.pageUrl()).queryParam("token", raw).toUriString();
            notifier.send(user, EmailTemplate.PASSWORD_RESET, Map.of("link", link, "minutes", props.tokenTtl().toMinutes()));
        });
    }

    /** Sets the new password and signs the user out everywhere (sessions and remembered devices). */
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        Instant now = clock.instant();
        PasswordResetToken token = tokenRepository.findByTokenHash(SecureTokens.sha256(rawToken))
                .filter(t -> t.isUsable(now))
                .orElseThrow(() -> new BadRequestException("INVALID_RESET_TOKEN",
                        "This reset link is invalid or has expired. Please request a new one."));

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        token.setUsedAt(now);
        refreshTokenService.revokeAll(user.getId());
        trustedDeviceService.revokeAll(user.getId());
    }
}
