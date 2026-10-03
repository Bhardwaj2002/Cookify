package com.cookify.user.dto;

import com.cookify.security.device.TrustedDevice;

import java.time.Instant;

/** A remembered browser, shown in account settings so the user can revoke it. */
public record TrustedDeviceResponse(Long id, String userAgent, Instant createdAt, Instant lastUsedAt, Instant expiresAt) {

    public static TrustedDeviceResponse from(TrustedDevice d) {
        return new TrustedDeviceResponse(d.getId(), d.getUserAgent(), d.getCreatedAt(), d.getLastUsedAt(), d.getExpiresAt());
    }
}
