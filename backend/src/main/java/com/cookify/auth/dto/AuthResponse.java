package com.cookify.auth.dto;

import com.cookify.user.dto.UserResponse;

/** Returned after signup / login / refresh. The refresh token is sent separately as an httpOnly cookie. */
public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {

    public static AuthResponse bearer(String accessToken, long expiresIn, UserResponse user) {
        return new AuthResponse(accessToken, "Bearer", expiresIn, user);
    }
}
