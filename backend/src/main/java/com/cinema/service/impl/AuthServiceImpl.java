package com.cinema.service.impl;

import com.cinema.config.JwtProperties;
import com.cinema.dto.auth.LoginRequest;
import com.cinema.dto.auth.RegisterRequest;
import com.cinema.dto.auth.UserResponse;
import com.cinema.entity.Member;
import com.cinema.entity.RefreshToken;
import com.cinema.entity.Role;
import com.cinema.entity.User;
import com.cinema.entity.UserStatus;
import com.cinema.exception.BusinessException;
import com.cinema.exception.InactiveAccountRefreshException;
import com.cinema.repository.MemberRepository;
import com.cinema.repository.RefreshTokenRepository;
import com.cinema.repository.RoleRepository;
import com.cinema.repository.UserRepository;
import com.cinema.security.JwtService;
import com.cinema.service.AuthService;
import com.cinema.service.AuthTokens;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final String MEMBER_ROLE = "MEMBER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            MemberRepository memberRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.memberRepository = memberRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    @Override
    public AuthTokens register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw BusinessException.conflict("Tên đăng nhập đã tồn tại");
        }
        if (StringUtils.hasText(request.email()) && userRepository.existsByEmail(request.email())) {
            throw BusinessException.conflict("Email đã được sử dụng");
        }
        if (StringUtils.hasText(request.phone()) && userRepository.existsByPhone(request.phone())) {
            throw BusinessException.conflict("Số điện thoại đã được sử dụng");
        }

        Role role = roleRepository.findByCode(MEMBER_ROLE)
                .orElseThrow(() -> new IllegalStateException("Chưa seed role MEMBER trong database"));

        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setEmail(normalize(request.email()));
        user.setPhone(normalize(request.phone()));
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(role);
        try {
            // Database constraints close the concurrent-registration race.
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw BusinessException.conflict("Username, email, or phone number already exists");
        }

        Member member = new Member();
        member.setUser(user);
        member.setMemberCode(String.format("MB%08d", user.getId()));
        memberRepository.save(member);

        return issueTokens(user, member);
    }

    @Override
    public AuthTokens login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> BusinessException.unauthorized("Sai tên đăng nhập hoặc mật khẩu"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw BusinessException.unauthorized("Sai tên đăng nhập hoặc mật khẩu");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw BusinessException.forbidden("Tài khoản đã bị khóa");
        }
        Member member = memberRepository.findByUserUsername(user.getUsername()).orElse(null);
        return issueTokens(user, member);
    }

    @Override
    @Transactional(noRollbackFor = InactiveAccountRefreshException.class)
    public AuthTokens refresh(String rawRefreshToken) {
        if (!StringUtils.hasText(rawRefreshToken)) {
            throw BusinessException.unauthorized("Thiếu refresh token");
        }
        RefreshToken storedToken = refreshTokenRepository.findByTokenHashForUpdate(hash(rawRefreshToken))
                .orElseThrow(() -> BusinessException.unauthorized("Refresh token không hợp lệ"));
        if (!storedToken.isUsable()) {
            storedToken.setRevoked(true);
            throw BusinessException.unauthorized("Refresh token đã hết hạn hoặc bị thu hồi");
        }

        User user = storedToken.getUser();
        if (user.getStatus() != UserStatus.ACTIVE) {
            refreshTokenRepository.revokeAllByUserId(user.getId());
            throw new InactiveAccountRefreshException();
        }
        storedToken.setRevoked(true);
        Member member = memberRepository.findByUserUsername(user.getUsername()).orElse(null);
        return issueTokens(user, member);
    }

    @Override
    public void logout(String rawRefreshToken) {
        if (!StringUtils.hasText(rawRefreshToken)) {
            return;
        }
        refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .ifPresent(token -> token.setRevoked(true));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse me(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> BusinessException.unauthorized("Không tìm thấy tài khoản"));
        Member member = memberRepository.findByUserUsername(username).orElse(null);
        return UserResponse.from(user, member);
    }

    private AuthTokens issueTokens(User user, Member member) {
        String accessToken = jwtService.generateAccessToken(user);
        String rawRefreshToken = generateRawRefreshToken();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hash(rawRefreshToken));
        refreshToken.setExpiresAt(Instant.now().plus(jwtProperties.getRefreshTokenTtl()));
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);

        return new AuthTokens(
                accessToken,
                rawRefreshToken,
                jwtService.accessTokenTtlSeconds(),
                UserResponse.from(user, member));
    }

    private String generateRawRefreshToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 không khả dụng", ex);
        }
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
