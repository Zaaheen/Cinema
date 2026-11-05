package com.cinema.controller;

import com.cinema.common.ApiResponse;
import com.cinema.config.JwtProperties;
import com.cinema.dto.auth.UserResponse;
import com.cinema.dto.member.ChangePasswordRequest;
import com.cinema.dto.member.UpdateProfileRequest;
import com.cinema.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/v1/members/me")
@PreAuthorize("hasRole('MEMBER')")
@Tag(name = "Tài khoản thành viên", description = "Hồ sơ và mật khẩu của thành viên hiện tại")
public class MemberController {

    private static final String REFRESH_COOKIE_PATH = "/api/v1/auth";

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    public MemberController(AuthService authService, JwtProperties jwtProperties) {
        this.authService = authService;
        this.jwtProperties = jwtProperties;
    }

    @GetMapping
    @Operation(summary = "Xem hồ sơ thành viên hiện tại")
    public ApiResponse<UserResponse> me(Authentication authentication) {
        return ApiResponse.ok(authService.me(authentication.getName()));
    }

    @PatchMapping
    @Operation(summary = "Cập nhật hồ sơ thành viên hiện tại")
    public ApiResponse<UserResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok("Cập nhật hồ sơ thành công", authService.updateProfile(authentication.getName(), request));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Đổi mật khẩu và thu hồi các phiên refresh token")
    public ApiResponse<Void> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletResponse response) {
        authService.changePassword(authentication.getName(), request);
        clearRefreshCookie(response);
        return ApiResponse.ok("Đổi mật khẩu thành công. Vui lòng đăng nhập lại.");
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(jwtProperties.getRefreshCookieName(), "")
                .httpOnly(true)
                .secure(jwtProperties.isRefreshCookieSecure())
                .sameSite(jwtProperties.getRefreshCookieSameSite())
                .path(REFRESH_COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
