package com.trading.security;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceImplTest {

    @Test
    void tokenRoundtripIssueAndValidate() {
        JwtService jwtService = new JwtServiceImpl("unit-test-secret", 15);

        UUID userId = UUID.randomUUID();
        String email = "trader@example.com";

        String token = jwtService.issueAccessToken(userId, email);

        assertTrue(jwtService.isTokenValid(token));
        assertEquals(userId, jwtService.extractUserId(token));
        assertEquals(email, jwtService.extractEmail(token));
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService jwtService = new JwtServiceImpl("unit-test-secret", 15);

        String token = jwtService.issueAccessToken(UUID.randomUUID(), "trader@example.com");
        String tamperedToken = token + "tamper";

        assertFalse(jwtService.isTokenValid(tamperedToken));
    }
}
