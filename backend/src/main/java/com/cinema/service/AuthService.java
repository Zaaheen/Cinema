package com.cinema.service;

import com.cinema.dto.auth.LoginRequest;
import com.cinema.dto.auth.RegisterRequest;
import com.cinema.dto.auth.UserResponse;

/** Contract for authentication and account-session operations. */
public interface AuthService {

    AuthTokens register(RegisterRequest request);

    AuthTokens login(LoginRequest request);

    AuthTokens refresh(String rawRefreshToken);

    void logout(String rawRefreshToken);

    UserResponse me(String username);
}
