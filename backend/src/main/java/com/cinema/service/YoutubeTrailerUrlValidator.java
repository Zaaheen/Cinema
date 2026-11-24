package com.cinema.service;

import com.cinema.exception.BusinessException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class YoutubeTrailerUrlValidator {
    private static final Set<String> YOUTUBE_HOSTS = Set.of("youtube.com", "www.youtube.com", "m.youtube.com");
    private static final Pattern VIDEO_ID = Pattern.compile("^[A-Za-z0-9_-]{11}$");

    public String normalize(String value) {
        if (value == null || value.isBlank() || value.length() > 500 || value.trim().length() != value.length()) {
            throw BusinessException.badRequest("trailerUrl không hợp lệ");
        }
        URI uri;
        try {
            uri = new URI(value);
        } catch (URISyntaxException exception) {
            throw BusinessException.badRequest("trailerUrl không hợp lệ");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                || uri.getRawUserInfo() != null || uri.getPort() != -1 || uri.getRawFragment() != null) {
            throw BusinessException.badRequest("trailerUrl không hợp lệ");
        }
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        String videoId;
        if ("youtu.be".equals(host)) {
            if (uri.getRawQuery() != null || !uri.getRawPath().matches("/[A-Za-z0-9_-]{11}")) {
                throw BusinessException.badRequest("trailerUrl không hợp lệ");
            }
            videoId = uri.getRawPath().substring(1);
        } else if (YOUTUBE_HOSTS.contains(host)) {
            if (!"/watch".equals(uri.getRawPath())) {
                throw BusinessException.badRequest("trailerUrl không hợp lệ");
            }
            videoId = queryVideoId(uri.getRawQuery());
        } else {
            throw BusinessException.badRequest("trailerUrl không hợp lệ");
        }
        if (!VIDEO_ID.matcher(videoId).matches()) {
            throw BusinessException.badRequest("trailerUrl không hợp lệ");
        }
        return "https://www.youtube.com/watch?v=" + videoId;
    }

    private String queryVideoId(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank() || rawQuery.contains("&") || !rawQuery.startsWith("v=")) {
            throw BusinessException.badRequest("trailerUrl không hợp lệ");
        }
        String videoId = rawQuery.substring(2);
        if (videoId.contains("=") || videoId.isBlank()) {
            throw BusinessException.badRequest("trailerUrl không hợp lệ");
        }
        return videoId;
    }
}
