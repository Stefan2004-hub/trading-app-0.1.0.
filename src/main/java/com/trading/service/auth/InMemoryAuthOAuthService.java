package com.trading.service.auth;

import com.trading.domain.enums.AuthProvider;
import com.trading.dto.auth.AuthResponse;
import com.trading.dto.auth.OAuthCallbackRequest;
import com.trading.security.JwtService;
import com.trading.security.RefreshTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryAuthOAuthService implements AuthOAuthService {
    private static final Logger log = LoggerFactory.getLogger(InMemoryAuthOAuthService.class);

    private final InMemoryAuthRegistrationService registrationService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final Map<String, UUID> googleLinksByProviderUserId = new ConcurrentHashMap<>();

    public InMemoryAuthOAuthService(
        InMemoryAuthRegistrationService registrationService,
        JwtService jwtService,
        RefreshTokenService refreshTokenService
    ) {
        this.registrationService = registrationService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    public AuthResponse handleGoogleCallback(OAuthCallbackRequest request) {
        Objects.requireNonNull(request, "request is required");
        if (isBlank(request.email()) || isBlank(request.providerUserId())) {
            throw new IllegalArgumentException("email and providerUserId are required");
        }
        log.warn("Google OAuth handled via InMemoryAuthOAuthService for email={}", request.email().trim());

        InMemoryAuthRegistrationService.RegisteredUser user =
            registrationService.getOrCreateGoogleUser(request.email(), request.preferredUsername());

        googleLinksByProviderUserId.put(request.providerUserId().trim(), user.id());

        String accessToken = jwtService.issueAccessToken(user.id(), user.email());
        String refreshToken = refreshTokenService.issueToken(user.id());

        AuthProvider provider = user.authProvider() == AuthProvider.LOCAL ? AuthProvider.LOCAL : AuthProvider.GOOGLE;
        return new AuthResponse(
            user.id(),
            user.email(),
            user.username(),
            provider,
            accessToken,
            refreshToken
        );
    }

    boolean isGoogleLinked(String providerUserId, UUID userId) {
        UUID linkedUserId = googleLinksByProviderUserId.get(providerUserId);
        return linkedUserId != null && linkedUserId.equals(userId);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
