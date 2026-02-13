package com.trading.service.auth;

import com.trading.domain.enums.AuthProvider;
import com.trading.dto.auth.AuthResponse;
import com.trading.dto.auth.OAuthCallbackRequest;
import com.trading.dto.auth.RegisterRequest;
import com.trading.dto.auth.RegisterResponse;
import com.trading.security.JwtService;
import com.trading.security.JwtServiceImpl;
import com.trading.security.RefreshTokenService;
import com.trading.security.RefreshTokenServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryAuthOAuthServiceTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final InMemoryAuthRegistrationService registrationService = new InMemoryAuthRegistrationService(passwordEncoder);
    private final JwtService jwtService = new JwtServiceImpl("oauth-unit-test-secret", 15);
    private final RefreshTokenService refreshTokenService = new RefreshTokenServiceImpl(30);
    private final InMemoryAuthOAuthService oauthService =
        new InMemoryAuthOAuthService(registrationService, jwtService, refreshTokenService);

    @Test
    void callbackCreatesGoogleUserAndReturnsValidAuthResponse() {
        AuthResponse response = oauthService.handleGoogleCallback(
            new OAuthCallbackRequest("googleuser@example.com", "google-sub-1", "googleuser")
        );

        assertNotNull(response.userId());
        assertEquals("googleuser@example.com", response.email());
        assertEquals(AuthProvider.GOOGLE, response.authProvider());
        assertTrue(jwtService.isTokenValid(response.accessToken()));
        assertTrue(refreshTokenService.isTokenValid(response.userId(), response.refreshToken()));
        assertTrue(oauthService.isGoogleLinked("google-sub-1", response.userId()));
    }

    @Test
    void callbackLinksToExistingLocalUserByEmail() {
        RegisterResponse localUser = registrationService.register(
            new RegisterRequest("local@example.com", "localuser", "Secret123!")
        );

        AuthResponse response = oauthService.handleGoogleCallback(
            new OAuthCallbackRequest("local@example.com", "google-sub-2", "ignoredname")
        );

        assertEquals(localUser.userId(), response.userId());
        assertEquals("local@example.com", response.email());
        assertEquals(AuthProvider.LOCAL, response.authProvider());
        assertTrue(jwtService.isTokenValid(response.accessToken()));
        assertTrue(refreshTokenService.isTokenValid(response.userId(), response.refreshToken()));
        assertTrue(oauthService.isGoogleLinked("google-sub-2", localUser.userId()));
    }
}
