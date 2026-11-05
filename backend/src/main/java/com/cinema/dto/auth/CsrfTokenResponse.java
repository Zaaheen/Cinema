package com.cinema.dto.auth;

/** Public bootstrap value paired with the CSRF cookie for cookie-auth endpoints. */
public record CsrfTokenResponse(String token) {
}
