package com.cinema.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cloudinary")
public record CloudinaryProperties(String cloudName, String apiKey, String apiSecret) {
    public boolean isConfigured() {
        return hasValue(cloudName) && hasValue(apiKey) && hasValue(apiSecret);
    }

    private static boolean hasValue(String value) {
        return value != null && !value.isBlank();
    }
}
