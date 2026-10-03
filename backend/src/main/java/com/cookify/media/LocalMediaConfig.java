package com.cookify.media;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

/** Serves locally stored media at {@code /media/**} (development only; production uses S3/CloudFront). */
@Configuration
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalMediaConfig implements WebMvcConfigurer {

    private final LocalStorageService storage;

    public LocalMediaConfig(LocalStorageService storage) {
        this.storage = storage;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // File names are random UUIDs and never reused, so they can be cached for a long time.
        registry.addResourceHandler("/media/**")
                .addResourceLocations(storage.root().toUri().toString())
                .setCacheControl(org.springframework.http.CacheControl.maxAge(Duration.ofDays(30)).cachePublic());
    }
}
