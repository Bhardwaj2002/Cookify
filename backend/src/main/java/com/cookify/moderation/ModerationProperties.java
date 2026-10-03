package com.cookify.moderation;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Moderation policy ({@code app.moderation.*}).
 *
 * @param warningsBeforeBan warnings that trigger an automatic ban (spec: 3)
 * @param autoBanDuration   how long the automatic suspension lasts
 */
@ConfigurationProperties(prefix = "app.moderation")
public record ModerationProperties(int warningsBeforeBan, Duration autoBanDuration) {

    public ModerationProperties {
        if (warningsBeforeBan <= 0) {
            warningsBeforeBan = 3;
        }
        if (autoBanDuration == null) {
            autoBanDuration = Duration.ofDays(30);
        }
    }
}
