package com.linktrack.service;

import com.linktrack.dto.request.LoginRequest;
import com.linktrack.dto.request.RegisterRequest;
import com.linktrack.dto.response.AuthResponse;
import com.linktrack.dto.response.UserResponse;
import com.linktrack.exception.BadRequestException;
import com.linktrack.model.RefreshToken;
import com.linktrack.model.User;
import com.linktrack.repository.RefreshTokenRepository;
import com.linktrack.repository.UserRepository;
import com.linktrack.security.JwtProperties;
import com.linktrack.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       JwtProperties jwtProperties,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new BadRequestException("El email ya está registrado");
        }
        var user = User.builder()
            .email(req.email())
            .password(passwordEncoder.encode(req.password()))
            .name(req.name())
            .build();
        userRepository.save(user);
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.email(), req.password()));
        var user = userRepository.findByEmail(req.email()).orElseThrow();
        refreshTokenRepository.deleteByUser(user);
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(String token) {
        var refreshToken = refreshTokenRepository.findByToken(token)
            .orElseThrow(() -> new BadRequestException("Refresh token inválido"));
        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new BadRequestException("Refresh token expirado");
        }
        var user = refreshToken.getUser();
        refreshTokenRepository.delete(refreshToken);
        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        var accessToken = jwtService.generateAccessToken(user);
        var rawRefresh = UUID.randomUUID().toString();
        var refreshToken = RefreshToken.builder()
            .user(user)
            .token(rawRefresh)
            .expiresAt(LocalDateTime.now().plusSeconds(jwtProperties.refreshExpirationMs() / 1000))
            .build();
        refreshTokenRepository.save(refreshToken);
        return AuthResponse.of(accessToken, rawRefresh,
            jwtProperties.expirationMs() / 1000, UserResponse.from(user));
    }
}
