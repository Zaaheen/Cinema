package com.cinema.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cinema.config.JwtProperties;
import com.cinema.dto.auth.RegisterRequest;
import com.cinema.entity.RefreshToken;
import com.cinema.entity.Role;
import com.cinema.entity.User;
import com.cinema.entity.UserStatus;
import com.cinema.exception.BusinessException;
import com.cinema.dto.member.ChangePasswordRequest;
import com.cinema.repository.MemberRepository;
import com.cinema.repository.RefreshTokenRepository;
import com.cinema.repository.RoleRepository;
import com.cinema.repository.UserRepository;
import com.cinema.security.JwtService;
import com.cinema.service.impl.AuthServiceImpl;
import java.time.Instant;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
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

    @Test
    void changePasswordVerifiesCurrentPasswordAndRevokesRefreshSessions() {
        User user = new User();
        user.setId(9L);
        user.setPasswordHash("existing-hash");
        when(userRepository.findByUsername("member"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current-password", "existing-hash"))
                .thenReturn(true);
        when(passwordEncoder.matches("new-password", "existing-hash"))
                .thenReturn(false);
        when(passwordEncoder.encode("new-password"))
                .thenReturn("new-hash");

        authService.changePassword("member", new ChangePasswordRequest("current-password", "new-password"));

        assertEquals("new-hash", user.getPasswordHash());
        verify(refreshTokenRepository).revokeAllByUserId(9L);
    }

    @Test
    void changePasswordRejectsIncorrectCurrentPassword() {
        User user = new User();
        user.setPasswordHash("existing-hash");
        when(userRepository.findByUsername("member"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "existing-hash"))
                .thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.changePassword("member", new ChangePasswordRequest("wrong-password", "new-password")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void registerTranslatesConcurrentUniqueConstraintViolationToConflict() {
        Role memberRole = new Role();
        memberRole.setCode("MEMBER");
        when(roleRepository.findByCode("MEMBER")).thenReturn(Optional.of(memberRole));
        when(userRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(User.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.register(
                new RegisterRequest("concurrent-user", "valid-password", "Concurrent User", "user@example.test", "0123456789")));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    void refreshTokenCannotBeConsumedTwiceAfterRotation() {
        User user = new User();
        user.setUsername("member");
        user.setStatus(UserStatus.ACTIVE);
        Role role = new Role();
        role.setCode("MEMBER");
        user.setRole(role);
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setExpiresAt(Instant.now().plusSeconds(60));
        token.setRevoked(false);
        when(refreshTokenRepository.findByTokenHashForUpdate(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(token));
        when(memberRepository.findByUserUsername(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.empty());
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtService.accessTokenTtlSeconds()).thenReturn(900L);
        when(jwtProperties.getRefreshTokenTtl()).thenReturn(Duration.ofDays(7));

        authService.refresh("one-time-refresh-token");
        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.refresh("one-time-refresh-token"));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertTrue(token.isRevoked());
    }
}
