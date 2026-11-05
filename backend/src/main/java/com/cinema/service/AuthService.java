package com.cinema.service;

import com.cinema.dto.auth.LoginRequest;
import com.cinema.dto.auth.RegisterRequest;
import com.cinema.dto.auth.UserResponse;
import com.cinema.dto.member.ChangePasswordRequest;
import com.cinema.dto.member.UpdateProfileRequest;

/** Contract for authentication and account-session operations. */
public interface AuthService {

    AuthTokens register(RegisterRequest request);

    AuthTokens login(LoginRequest request);

    AuthTokens refresh(String rawRefreshToken);

    void logout(String rawRefreshToken);

    UserResponse me(String username);

    UserResponse updateProfile(String username, UpdateProfileRequest request);

    void changePassword(String username, ChangePasswordRequest request);
}
