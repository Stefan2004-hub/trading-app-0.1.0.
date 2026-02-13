package com.trading.service.auth;

import com.trading.domain.enums.AuthProvider;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthRegistrationService authRegistrationService;
    @Mock
    private AuthLoginService authLoginService;
    @Mock
    private AuthOAuthService authOAuthService;
    @Mock
    private AuthLogoutService authLogoutService;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private InMemoryAuthRegistrationService registrationService;

    private AuthServiceImpl authService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        authService = new AuthServiceImpl(
            authRegistrationService,
            authLoginService,
            authOAuthService,
            authLogoutService,
            jwtService,
            refreshTokenService,
            registrationService
        );
    }

    @Test
    void registerDelegatesToRegistrationService() {
        RegisterRequest request = new RegisterRequest("trader@example.com", "satoshi", "Secret123!");
        RegisterResponse expected = new RegisterResponse(userId, request.email(), request.username(), AuthProvider.LOCAL);
        when(authRegistrationService.register(request)).thenReturn(expected);

        RegisterResponse response = authService.register(request);

        assertEquals(expected, response);
        verify(authRegistrationService).register(request);
    }

    @Test
    void loginDelegatesToLoginService() {
        LoginRequest request = new LoginRequest("satoshi", "Secret123!");
        LoginResponse expected = new LoginResponse(userId, "trader@example.com", "satoshi", AuthProvider.LOCAL);
        when(authLoginService.login(request)).thenReturn(expected);

        LoginResponse response = authService.login(request);

        assertEquals(expected, response);
        verify(authLoginService).login(request);
    }

    @Test
    void handleGoogleCallbackDelegatesToOauthService() {
        OAuthCallbackRequest request = new OAuthCallbackRequest("google@example.com", "google-sub", "googleuser");
        AuthResponse expected = new AuthResponse(
            userId,
            "google@example.com",
            "googleuser",
            AuthProvider.GOOGLE,
            "access-token",
            "refresh-token"
        );
        when(authOAuthService.handleGoogleCallback(request)).thenReturn(expected);

        AuthResponse response = authService.handleGoogleCallback(request);

        assertEquals(expected, response);
        verify(authOAuthService).handleGoogleCallback(request);
    }

    @Test
    void refreshBuildsAccessAndRefreshTokensFromCurrentUser() {
        InMemoryAuthRegistrationService.RegisteredUser registeredUser =
            new InMemoryAuthRegistrationService.RegisteredUser(
                userId,
                "trader@example.com",
                "satoshi",
                "hash",
                AuthProvider.LOCAL
            );
        when(registrationService.findByUserId(userId)).thenReturn(registeredUser);
        when(refreshTokenService.rotateToken(userId, "refresh-token")).thenReturn("rotated-refresh-token");
        when(jwtService.issueAccessToken(userId, "trader@example.com")).thenReturn("new-access-token");

        RefreshResponse response = authService.refresh(userId, new RefreshRequest("refresh-token"));

        assertEquals("new-access-token", response.accessToken());
        assertEquals("rotated-refresh-token", response.refreshToken());
        verify(refreshTokenService).rotateToken(userId, "refresh-token");
        verify(jwtService).issueAccessToken(userId, "trader@example.com");
    }

    @Test
    void refreshRejectsNullRefreshToken() {
        NullPointerException ex = assertThrows(
            NullPointerException.class,
            () -> authService.refresh(userId, new RefreshRequest(null))
        );
        assertEquals("refreshToken is required", ex.getMessage());
    }

    @Test
    void logoutDelegatesToLogoutService() {
        LogoutRequest request = new LogoutRequest("refresh-token");

        authService.logout(userId, request);

        verify(authLogoutService).logout(userId, "refresh-token");
    }

    @Test
    void meReturnsRegisteredUserDetails() {
        InMemoryAuthRegistrationService.RegisteredUser registeredUser =
            new InMemoryAuthRegistrationService.RegisteredUser(
                userId,
                "trader@example.com",
                "satoshi",
                "hash",
                AuthProvider.LOCAL
            );
        when(registrationService.findByUserId(userId)).thenReturn(registeredUser);

        MeResponse response = authService.me(userId);

        assertEquals(userId, response.userId());
        assertEquals("trader@example.com", response.email());
        assertEquals("satoshi", response.username());
        assertEquals(AuthProvider.LOCAL, response.authProvider());
    }

    @Test
    void meRejectsUnknownUser() {
        when(registrationService.findByUserId(userId)).thenReturn(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.me(userId));

        assertEquals("User not found: " + userId, ex.getMessage());
    }
}
