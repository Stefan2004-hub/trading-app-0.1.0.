package com.trading.security;

import java.util.UUID;

public interface RefreshTokenService {

    String issueToken(UUID userId);

    String rotateToken(UUID userId, String refreshToken);

    boolean isTokenValid(UUID userId, String refreshToken);

    void revokeToken(UUID userId, String refreshToken);
}
