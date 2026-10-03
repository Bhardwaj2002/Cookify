package com.cookify.auth.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Password reset settings ({@code app.auth.password-reset.*}).
 *
 * @param tokenTtl how long the emailed link stays valid
 * @param pageUrl  frontend page that receives {@code ?token=...}
 */
@ConfigurationProperties(prefix = "app.auth.password-reset")
public record PasswordResetProperties(Duration tokenTtl, String pageUrl) {
}
