package com.cookify.auth.controller;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Refresh-cookie settings ({@code app.auth.cookie.*}).
 *
 * @param secure   send only over HTTPS (true in production)
 * @param sameSite Lax / Strict / None
 */
@ConfigurationProperties(prefix = "app.auth.cookie")
public record AuthCookieProperties(boolean secure, String sameSite) {

    public AuthCookieProperties {
        if (sameSite == null || sameSite.isBlank()) {
            sameSite = "Lax";
        }
    }
}
