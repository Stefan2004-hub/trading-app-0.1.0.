package com.trading.dto.user;

import com.trading.domain.enums.BuyInputMode;

import java.util.UUID;

public record UserPreferenceResponse(
    UUID userId,
    BuyInputMode defaultBuyInputMode
) {
}
