package com.cookify.security.otp;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Email 2FA settings ({@code app.auth.two-factor.*}).
 *
 * @param codeTtl          how long a login code is valid
 * @param maxAttempts      wrong guesses allowed before the challenge is burned
 * @param trustedDeviceTtl how long "remember this device" skips the code
 */
@ConfigurationProperties(prefix = "app.auth.two-factor")
public record TwoFactorProperties(Duration codeTtl, int maxAttempts, Duration trustedDeviceTtl) {
}
