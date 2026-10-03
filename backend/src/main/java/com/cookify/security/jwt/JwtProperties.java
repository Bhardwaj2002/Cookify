package com.cookify.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.util.Base64;

/**
 * JWT settings ({@code app.jwt.*}).
 *
 * @param secret          Base64-encoded HMAC-SHA256 key, at least 256 bits
 * @param issuer          value of the {@code iss} claim
 * @param accessTokenTtl  lifetime of access tokens (short, e.g. 15m)
 * @param refreshTokenTtl lifetime of refresh tokens / cookie (long, e.g. 30d)
 * @param refreshReuseGrace how long a just-rotated refresh token is still accepted, so parallel
 *                          page loads or tabs refreshing at the same moment don't log the user out
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, String issuer, Duration accessTokenTtl, Duration refreshTokenTtl,
                            Duration refreshReuseGrace) {

    public JwtProperties {
        if (secret == null || Base64.getDecoder().decode(secret).length < 32) {
            throw new IllegalStateException("app.jwt.secret must be a Base64 key of at least 256 bits");
        }
        if (refreshReuseGrace == null) {
            refreshReuseGrace = Duration.ZERO;
        }
    }

    public SecretKey secretKey() {
        return new SecretKeySpec(Base64.getDecoder().decode(secret), "HmacSHA256");
    }
}
