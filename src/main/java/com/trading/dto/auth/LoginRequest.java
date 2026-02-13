package com.trading.dto.auth;

public record LoginRequest(
    String identifier,
    String password
) {
}
