package com.trading.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
    @NotBlank(message = "refreshToken is required")
    String refreshToken
) {
}
