package com.trading.service.auth;

import java.util.UUID;

public interface AuthLogoutService {

    void logout(UUID userId, String refreshToken);
}
