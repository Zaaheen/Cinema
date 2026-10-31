package com.cinema.service;

import com.cinema.dto.auth.UserResponse;

public record AuthTokens(
        String accessToken,
        String refreshToken,
        long expiresIn,
        UserResponse user) {
}
