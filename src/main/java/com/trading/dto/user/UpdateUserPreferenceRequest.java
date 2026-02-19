package com.trading.dto.user;

import com.trading.domain.enums.BuyInputMode;
import jakarta.validation.constraints.NotNull;

public record UpdateUserPreferenceRequest(
    @NotNull(message = "defaultBuyInputMode is required")
    BuyInputMode defaultBuyInputMode
) {
}
