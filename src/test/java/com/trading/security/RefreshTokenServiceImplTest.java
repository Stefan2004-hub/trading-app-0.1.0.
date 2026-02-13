package com.trading.security;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefreshTokenServiceImplTest {

    @Test
    void oldRefreshTokenIsRejectedAfterRotation() {
        RefreshTokenService refreshTokenService = new RefreshTokenServiceImpl(30);
        UUID userId = UUID.randomUUID();

        String firstToken = refreshTokenService.issueToken(userId);
        assertTrue(refreshTokenService.isTokenValid(userId, firstToken));

        String rotatedToken = refreshTokenService.rotateToken(userId, firstToken);

        assertNotEquals(firstToken, rotatedToken);
        assertFalse(refreshTokenService.isTokenValid(userId, firstToken));
        assertTrue(refreshTokenService.isTokenValid(userId, rotatedToken));
    }
}
