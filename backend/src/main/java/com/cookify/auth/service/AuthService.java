package com.cookify.auth.service;

import com.cookify.auth.dto.AuthResponse;
import com.cookify.auth.dto.LoginRequest;
import com.cookify.auth.dto.SignupRequest;
import com.cookify.auth.dto.TwoFactorChallengeResponse;
import com.cookify.auth.dto.VerifyOtpRequest;
import com.cookify.common.exception.ConflictException;
import com.cookify.common.exception.ForbiddenException;
import com.cookify.common.exception.UnauthorizedException;
import com.cookify.common.util.Masking;
import com.cookify.security.device.TrustedDeviceService;
import com.cookify.security.jwt.JwtService;
import com.cookify.security.otp.OtpService;
import com.cookify.user.dto.UserResponse;
import com.cookify.user.entity.User;
import com.cookify.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final OtpService otpService;
    private final TrustedDeviceService trustedDeviceService;
    private final Clock clock;

    /**
     * Pre-computed hash used when the login identifier matches no user, so that a wrong
     * username takes as long as a wrong password (prevents user enumeration by timing).
     */
    private String dummyHash;

    /** Creates the account and logs the user straight in ("Create account + redirect to home page"). */
    @Transactional
    public AuthResult signup(SignupRequest req, String userAgent) {
        String username = req.username().trim();
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        String phone = blankToNull(req.phone());

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException("USERNAME_TAKEN", "This username is taken, try again");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("EMAIL_TAKEN", "An account with this email already exists");
        }
        if (phone != null && userRepository.existsByPhone(phone)) {
            throw new ConflictException("PHONE_TAKEN", "An account with this phone number already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setFirstName(blankToNull(req.firstName()));
        user.setLastName(blankToNull(req.lastName()));

        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Two concurrent signups with the same username/email: the unique index wins.
            throw new ConflictException("ACCOUNT_EXISTS", "Username, email or phone is already in use");
        }
        return issueTokens(user, userAgent, null);
    }

    /**
     * Step 1 of login. If 2FA is on and this browser is not a trusted device, a code is emailed
     * and a challenge is returned instead of tokens.
     */
    // noRollbackFor: keep the session revocation done by ensureNotBanned() for banned users.
    @Transactional(noRollbackFor = {UnauthorizedException.class, ForbiddenException.class})
    public LoginOutcome login(LoginRequest req, String deviceToken, String userAgent) {
        User user = userRepository.findByLoginIdentifier(req.identifier().trim()).orElse(null);

        if (user == null) {
            passwordEncoder.matches(req.password(), dummyHash());
            throw invalidCredentials();
        }
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        ensureNotBanned(user);

        if (user.isTwoFactorEnabled() && !trustedDeviceService.isTrusted(user, deviceToken)) {
            OtpService.Challenge challenge = otpService.createLoginChallenge(user);
            return new LoginOutcome.TwoFactorRequired(TwoFactorChallengeResponse.of(
                    challenge.challengeId(), Masking.email(user.getEmail()), challenge.expiresInSeconds()));
        }
        return new LoginOutcome.Authenticated(issueTokens(user, userAgent, null));
    }

    /** Step 2 of login: check the emailed code, optionally remember this browser, issue tokens. */
    @Transactional(noRollbackFor = {UnauthorizedException.class, ForbiddenException.class})
    public AuthResult verifyOtp(VerifyOtpRequest req, String userAgent) {
        User user = otpService.verifyLoginChallenge(req.challengeId(), req.code());
        ensureNotBanned(user);
        String deviceToken = req.remember() ? trustedDeviceService.trust(user, userAgent) : null;
        return issueTokens(user, userAgent, deviceToken);
    }

    /** Exchanges a valid refresh token (cookie) for a new access token and a rotated refresh token. */
    @Transactional(noRollbackFor = {UnauthorizedException.class, ForbiddenException.class})
    public AuthResult refresh(String refreshToken, String userAgent) {
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(refreshToken, userAgent);
        User user = rotation.user();
        ensureNotBanned(user);
        return new AuthResult(authBody(user), rotation.newToken());
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    private AuthResult issueTokens(User user, String userAgent, String trustedDeviceToken) {
        String refreshToken = refreshTokenService.issue(user, userAgent);
        return new AuthResult(authBody(user), refreshToken, trustedDeviceToken);
    }

    private AuthResponse authBody(User user) {
        return AuthResponse.bearer(jwtService.issueAccessToken(user), jwtService.accessTokenTtlSeconds(), UserResponse.from(user));
    }

    private void ensureNotBanned(User user) {
        if (user.isBannedAt(clock.instant())) {
            refreshTokenService.revokeAll(user.getId());
            throw new ForbiddenException("ACCOUNT_BANNED", "This account has been suspended");
        }
    }

    private String dummyHash() {
        if (dummyHash == null) {
            dummyHash = passwordEncoder.encode("dummy-password-for-timing");
        }
        return dummyHash;
    }

    private static UnauthorizedException invalidCredentials() {
        return new UnauthorizedException("INVALID_CREDENTIALS", "Incorrect username/email or password");
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
