package com.cinema.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cinema.entity.RefreshToken;
import com.cinema.entity.Role;
import com.cinema.entity.User;
import com.cinema.entity.UserStatus;
import com.cinema.exception.BusinessException;
import com.cinema.repository.RefreshTokenRepository;
import com.cinema.repository.RoleRepository;
import com.cinema.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AuthRefreshPersistenceTests {

    @Autowired private AuthService authService;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;

    @Test
    void inactiveRefreshCommitsSessionRevocationBeforeReturningUnauthorized() throws Exception {
        Role role = roleRepository.findByCode("MEMBER").orElseGet(() -> {
            Role newRole = new Role();
            newRole.setCode("MEMBER");
            newRole.setName("Member");
            return roleRepository.save(newRole);
        });
        User user = new User();
        user.setUsername("locked-refresh-user");
        user.setPasswordHash("hash");
        user.setFullName("Locked Refresh User");
        user.setRole(role);
        user.setStatus(UserStatus.LOCKED);
        user = userRepository.saveAndFlush(user);

        String rawToken = "refresh-token-for-locked-user";
        String secondRawToken = "second-refresh-token-for-locked-user";
        refreshTokenRepository.saveAndFlush(refreshToken(user, rawToken));
        refreshTokenRepository.saveAndFlush(refreshToken(user, secondRawToken));

        assertThrows(BusinessException.class, () -> authService.refresh(rawToken));

        assertTrue(refreshTokenRepository.findByTokenHash(sha256(rawToken)).orElseThrow().isRevoked());
        assertTrue(refreshTokenRepository.findByTokenHash(sha256(secondRawToken)).orElseThrow().isRevoked());
    }

    private RefreshToken refreshToken(User user, String rawToken) throws Exception {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(sha256(rawToken));
        token.setExpiresAt(Instant.now().plusSeconds(60));
        token.setRevoked(false);
        return token;
    }

    private String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
