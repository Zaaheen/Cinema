package com.cinema.security;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.http.MediaType;

final class SecurityErrorWriter {

    private SecurityErrorWriter() {
    }

    static void write(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String body = "{\"success\":false,\"message\":\"%s\",\"errors\":[],\"timestamp\":\"%s\"}"
                .formatted(message, Instant.now());
        response.getWriter().write(body);
    }
}
