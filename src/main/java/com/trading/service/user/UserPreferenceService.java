package com.trading.service.user;

import com.trading.domain.enums.BuyInputMode;
import com.trading.dto.user.UserPreferenceResponse;

import java.util.UUID;

public interface UserPreferenceService {

    UserPreferenceResponse get(UUID userId);

    UserPreferenceResponse updateDefaultBuyInputMode(UUID userId, BuyInputMode mode);
}
