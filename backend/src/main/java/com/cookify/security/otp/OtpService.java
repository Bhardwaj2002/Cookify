package com.cookify.security.otp;

import com.cookify.common.exception.UnauthorizedException;
import com.cookify.common.util.SecureTokens;
import com.cookify.notification.EmailTemplate;
import com.cookify.notification.Notifier;
import com.cookify.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;

/**
 * Email-based 2FA. After a correct password, the user receives a 6-digit code by email and the
 * client receives an opaque challenge id; both are needed to finish logging in.
 */
@Service
@RequiredArgsConstructor
public class OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpCodeRepository repository;
    private final TwoFactorProperties props;
    private final Notifier notifier;
    private final Clock clock;

    /** Returned to the client so it can show the code-entry step. */
    public record Challenge(String challengeId, long expiresInSeconds) {
    }

    @Transactional
    public Challenge createLoginChallenge(User user) {
        Instant now = clock.instant();
        repository.invalidatePending(user.getId(), now);

        String challengeId = SecureTokens.generate();
        String code = "%06d".formatted(RANDOM.nextInt(1_000_000));

        OtpCode otp = new OtpCode();
        otp.setUser(user);
        otp.setChallengeHash(SecureTokens.sha256(challengeId));
        otp.setCodeHash(SecureTokens.sha256(code));
        otp.setExpiresAt(now.plus(props.codeTtl()));
        repository.save(otp);

        notifier.send(user, EmailTemplate.LOGIN_OTP, Map.of("code", code, "minutes", props.codeTtl().toMinutes()));
        return new Challenge(challengeId, props.codeTtl().toSeconds());
    }

    /**
     * Checks the code for a challenge and returns its user. Wrong codes count towards
     * {@code maxAttempts}; the counter is committed even though the request fails.
     */
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public User verifyLoginChallenge(String challengeId, String code) {
        Instant now = clock.instant();
        OtpCode otp = repository.findByChallengeHash(SecureTokens.sha256(challengeId))
                .orElseThrow(OtpService::expired);

        if (!otp.isUsable(now, props.maxAttempts())) {
            throw expired();
        }
        if (!constantTimeEquals(otp.getCodeHash(), SecureTokens.sha256(code))) {
            otp.setAttempts(otp.getAttempts() + 1);
            int left = props.maxAttempts() - otp.getAttempts();
            if (left <= 0) {
                throw new UnauthorizedException("OTP_EXPIRED", "Too many wrong codes. Please log in again.");
            }
            throw new UnauthorizedException("INVALID_OTP", "Incorrect code. " + left + " attempt(s) left.");
        }
        otp.setUsedAt(now);
        return otp.getUser();
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private static UnauthorizedException expired() {
        return new UnauthorizedException("OTP_EXPIRED", "This code has expired. Please log in again.");
    }
}
