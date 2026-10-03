package com.cookify.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code app.demo-data.enabled}: load sample cooks and recipes on startup (dev / demo deployments). */
@ConfigurationProperties(prefix = "app.demo-data")
public record DemoDataProperties(boolean enabled) {
}
