package com.trading.service.auth;

import com.trading.dto.auth.AuthResponse;
import com.trading.dto.auth.LoginRequest;
import com.trading.dto.auth.LoginResponse;
import com.trading.dto.auth.LogoutRequest;
import com.trading.dto.auth.MeResponse;
import com.trading.dto.auth.OAuthCallbackRequest;
import com.trading.dto.auth.RefreshRequest;
import com.trading.dto.auth.RefreshResponse;
import com.trading.dto.auth.RegisterRequest;
import com.trading.dto.auth.RegisterResponse;
import com.trading.security.JwtService;
import com.trading.security.RefreshTokenService;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthRegistrationService authRegistrationService;
    private final AuthLoginService authLoginService;
    private final AuthOAuthService authOAuthService;
    private final AuthLogoutService authLogoutService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final InMemoryAuthRegistrationService registrationService;

    public AuthServiceImpl(
        AuthRegistrationService authRegistrationService,
        AuthLoginService authLoginService,
        AuthOAuthService authOAuthService,
        AuthLogoutService authLogoutService,
        JwtService jwtService,
        RefreshTokenService refreshTokenService,
        InMemoryAuthRegistrationService registrationService
    ) {
        this.authRegistrationService = authRegistrationService;
        this.authLoginService = authLoginService;
        this.authOAuthService = authOAuthService;
        this.authLogoutService = authLogoutService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.registrationService = registrationService;
    }

    @Override
    public RegisterResponse register(RegisterRequest request) {
        return authRegistrationService.register(request);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        return authLoginService.login(request);
    }

    @Override
    public AuthResponse handleGoogleCallback(OAuthCallbackRequest request) {
        return authOAuthService.handleGoogleCallback(request);
    }

    @Override
    public RefreshResponse refresh(UUID userId, RefreshRequest request) {
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(request, "request is required");
        Objects.requireNonNull(request.refreshToken(), "refreshToken is required");

        MeResponse me = me(userId);
        String newRefreshToken = refreshTokenService.rotateToken(userId, request.refreshToken());
        String accessToken = jwtService.issueAccessToken(userId, me.email());
        return new RefreshResponse(accessToken, newRefreshToken);
    }

    @Override
    public void logout(UUID userId, LogoutRequest request) {
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(request, "request is required");
        Objects.requireNonNull(request.refreshToken(), "refreshToken is required");
        authLogoutService.logout(userId, request.refreshToken());
    }

    @Override
    public MeResponse me(UUID userId) {
        Objects.requireNonNull(userId, "userId is required");
        InMemoryAuthRegistrationService.RegisteredUser user = registrationService.findByUserId(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + userId);
        }
        return new MeResponse(user.id(), user.email(), user.username(), user.authProvider());
    }
}
