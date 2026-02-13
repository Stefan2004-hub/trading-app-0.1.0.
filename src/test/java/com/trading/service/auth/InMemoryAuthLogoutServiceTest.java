package com.trading.service.auth;

import com.trading.security.RefreshTokenService;
import com.trading.security.RefreshTokenServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryAuthLogoutServiceTest {

    @Test
    void revokedTokenCannotRefreshSession() {
        RefreshTokenService refreshTokenService = new RefreshTokenServiceImpl(30);
        AuthLogoutService logoutService = new InMemoryAuthLogoutService(refreshTokenService);

        UUID userId = UUID.randomUUID();
        String refreshToken = refreshTokenService.issueToken(userId);

        logoutService.logout(userId, refreshToken);

        assertFalse(refreshTokenService.isTokenValid(userId, refreshToken));
        assertThrows(
            IllegalArgumentException.class,
            () -> refreshTokenService.rotateToken(userId, refreshToken)
        );
    }
}
