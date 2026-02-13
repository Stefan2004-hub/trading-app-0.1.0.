package com.trading.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "identifier is required")
    String identifier,
    @NotBlank(message = "password is required")
    String password
) {
}
