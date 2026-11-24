package com.cinema.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cloudinary")
public record CloudinaryProperties(
        String cloudName,
        String apiKey,
        String apiSecret,
        Duration connectTimeout,
        Duration readTimeout) {

    public boolean isConfigured() {
        return hasValue(cloudName) && hasValue(apiKey) && hasValue(apiSecret);
    }

    public Duration effectiveConnectTimeout() {
        return connectTimeout == null ? Duration.ofSeconds(3) : connectTimeout;
    }

    public Duration effectiveReadTimeout() {
        return readTimeout == null ? Duration.ofSeconds(10) : readTimeout;
    }

    private static boolean hasValue(String value) {
        return value != null && !value.isBlank();
    }
}
