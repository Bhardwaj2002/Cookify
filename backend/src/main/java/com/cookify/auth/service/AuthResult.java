package com.cookify.auth.service;

import com.cookify.auth.dto.AuthResponse;

/**
 * What the auth service hands to the controller: the JSON body, the raw refresh token for the
 * session cookie, and (optionally) a new trusted-device token for the device cookie.
 */
public record AuthResult(AuthResponse body, String refreshToken, String trustedDeviceToken) {

    public AuthResult(AuthResponse body, String refreshToken) {
        this(body, refreshToken, null);
    }
}
