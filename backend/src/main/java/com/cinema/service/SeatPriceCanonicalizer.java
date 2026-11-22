package com.cinema.service;

import java.util.Locale;
import org.springframework.util.StringUtils;

public final class SeatPriceCanonicalizer {
    private SeatPriceCanonicalizer() {}

    public static String display(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    public static String key(String value) {
        String normalized = display(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }
}
