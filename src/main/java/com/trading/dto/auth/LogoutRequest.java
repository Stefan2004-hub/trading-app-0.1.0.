package com.trading.dto.auth;

public record LogoutRequest(
    String refreshToken
) {
}
