package com.trading.security;

import java.util.UUID;

public interface JwtService {

    String issueAccessToken(UUID userId, String email);

    UUID extractUserId(String token);

    String extractEmail(String token);

    boolean isTokenValid(String token);
}
