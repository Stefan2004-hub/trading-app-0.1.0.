package com.trading.dto.auth;

import com.trading.domain.enums.AuthProvider;

import java.util.UUID;

public record MeResponse(
    UUID userId,
    String email,
    String username,
    AuthProvider authProvider
) {
}
