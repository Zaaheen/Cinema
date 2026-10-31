package com.cinema.controller;

import com.cinema.common.ApiResponse;
import com.cinema.config.JwtProperties;
import com.cinema.dto.auth.AuthResponse;
import com.cinema.dto.auth.LoginRequest;
import com.cinema.dto.auth.RegisterRequest;
import com.cinema.dto.auth.UserResponse;
import com.cinema.service.AuthService;
import com.cinema.service.AuthTokens;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.Arrays;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Xác thực", description = "Đăng ký, đăng nhập, làm mới và thu hồi token")
public class AuthController {

    private static final String COOKIE_PATH = "/api/v1/auth";

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    public AuthController(AuthService authService, JwtProperties jwtProperties) {
        this.authService = authService;
        this.jwtProperties = jwtProperties;
    }

    @PostMapping("/register")
    @Operation(summary = "Đăng ký tài khoản thành viên")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {
        AuthTokens tokens = authService.register(request);
        writeRefreshCookie(response, tokens.refreshToken());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Đăng ký thành công", toResponse(tokens)));
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập và nhận access token")
    public ApiResponse<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        AuthTokens tokens = authService.login(request);
        writeRefreshCookie(response, tokens.refreshToken());
        return ApiResponse.ok("Đăng nhập thành công", toResponse(tokens));
    }

    @GetMapping("/csrf")
    @Operation(summary = "Initialize a CSRF token for refresh and logout")
    public ApiResponse<Void> csrf(@RequestAttribute(name = "_csrf") CsrfToken csrfToken) {
        // Accessing the deferred token causes Spring Security to write XSRF-TOKEN.
        csrfToken.getToken();
        return ApiResponse.ok(null);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Làm mới access token bằng refresh token trong cookie")
    public ApiResponse<AuthResponse> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {
        AuthTokens tokens = authService.refresh(readRefreshCookie(request));
        writeRefreshCookie(response, tokens.refreshToken());
        return ApiResponse.ok(toResponse(tokens));
    }

    @PostMapping("/logout")
    @Operation(summary = "Đăng xuất và thu hồi refresh token")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(readRefreshCookie(request));
        clearRefreshCookie(response);
        return ApiResponse.ok("Đăng xuất thành công");
    }

    @GetMapping("/me")
    @Operation(summary = "Thông tin tài khoản đang đăng nhập")
    public ApiResponse<UserResponse> me(Authentication authentication) {
        return ApiResponse.ok(authService.me(authentication.getName()));
    }

    private AuthResponse toResponse(AuthTokens tokens) {
        return new AuthResponse(
                tokens.accessToken(),
                "Bearer",
                tokens.expiresIn(),
                tokens.user());
    }

    private void writeRefreshCookie(HttpServletResponse response, String rawToken) {
        ResponseCookie cookie = ResponseCookie
                .from(jwtProperties.getRefreshCookieName(), rawToken)
                .httpOnly(true)
                .secure(jwtProperties.isRefreshCookieSecure())
                .sameSite(jwtProperties.getRefreshCookieSameSite())
                .path(COOKIE_PATH)
                .maxAge(jwtProperties.getRefreshTokenTtl())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie
                .from(jwtProperties.getRefreshCookieName(), "")
                .httpOnly(true)
                .secure(jwtProperties.isRefreshCookieSecure())
                .sameSite(jwtProperties.getRefreshCookieSameSite())
                .path(COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String readRefreshCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
                .filter(cookie -> cookie.getName().equals(jwtProperties.getRefreshCookieName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
