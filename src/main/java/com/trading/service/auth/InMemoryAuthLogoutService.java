package com.trading.service.auth;

import com.trading.security.RefreshTokenService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class InMemoryAuthLogoutService implements AuthLogoutService {

    private final RefreshTokenService refreshTokenService;

    public InMemoryAuthLogoutService(RefreshTokenService refreshTokenService) {
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    public void logout(UUID userId, String refreshToken) {
        refreshTokenService.revokeToken(userId, refreshToken);
    }
}
