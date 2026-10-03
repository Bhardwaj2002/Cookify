package com.cookify.media;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Media storage settings ({@code app.storage.*}).
 *
 * @param localDir      directory for {@link LocalStorageService}
 * @param publicBaseUrl URL prefix files are served from, e.g. {@code http://localhost:8080/media}
 */
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(String localDir, String publicBaseUrl) {
}
