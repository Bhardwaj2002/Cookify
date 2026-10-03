package com.cookify.auth.controller;

import com.cookify.security.jwt.JwtProperties;
import com.cookify.security.otp.TwoFactorProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Builds the two httpOnly auth cookies. Both are scoped to {@code /api/auth}, so the browser only
 * sends them to the auth endpoints, and JavaScript can never read them.
 * <ul>
 *   <li>{@value #REFRESH} – the login session (refresh token)</li>
 *   <li>{@value #DEVICE} – "remember this device" token that skips the 2FA code</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class AuthCookies {

    public static final String REFRESH = "cookify_refresh";
    public static final String DEVICE = "cookify_device";
    private static final String PATH = "/api/auth";

    private final AuthCookieProperties cookieProps;
    private final JwtProperties jwtProps;
    private final TwoFactorProperties twoFactorProps;

    public ResponseCookie refresh(String token) {
        return build(REFRESH, token, jwtProps.refreshTokenTtl());
    }

    public ResponseCookie clearRefresh() {
        return build(REFRESH, "", Duration.ZERO);
    }

    public ResponseCookie device(String token) {
        return build(DEVICE, token, twoFactorProps.trustedDeviceTtl());
    }

    private ResponseCookie build(String name, String value, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieProps.secure())
                .sameSite(cookieProps.sameSite())
                .path(PATH)
                .maxAge(maxAge)
                .build();
    }
}
