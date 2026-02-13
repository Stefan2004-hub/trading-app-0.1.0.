package com.trading.dto.auth;

import com.trading.domain.enums.AuthProvider;

import java.util.UUID;

public record AuthResponse(
    UUID userId,
    String email,
    String username,
    AuthProvider authProvider,
    String accessToken,
    String refreshToken
) {
}
