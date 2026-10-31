package com.cinema.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cinema.config.JwtProperties;
import com.cinema.entity.RefreshToken;
import com.cinema.entity.User;
import com.cinema.entity.UserStatus;
import com.cinema.exception.BusinessException;
import com.cinema.repository.MemberRepository;
import com.cinema.repository.RefreshTokenRepository;
import com.cinema.repository.RoleRepository;
import com.cinema.repository.UserRepository;
import com.cinema.security.JwtService;
import com.cinema.service.impl.AuthServiceImpl;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTests {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private JwtProperties jwtProperties;

    @InjectMocks private AuthServiceImpl authService;

    @Test
    void refreshRevokesAllSessionsWhenUserIsNotActive() {
        User user = new User();
        user.setId(42L);
        user.setStatus(UserStatus.LOCKED);
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setExpiresAt(Instant.now().plusSeconds(60));
        token.setRevoked(false);
        when(refreshTokenRepository.findByTokenHashForUpdate(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(token));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.refresh("refresh-token"));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        verify(refreshTokenRepository).revokeAllByUserId(42L);
    }
}
