package com.trading.dto.auth;

public record RefreshResponse(
    String accessToken,
    String refreshToken
) {
}
