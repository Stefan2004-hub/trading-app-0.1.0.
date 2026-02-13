package com.trading.dto.auth;

public record OAuthCallbackRequest(
    String email,
    String providerUserId,
    String preferredUsername
) {
}
