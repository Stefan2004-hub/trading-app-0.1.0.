package com.trading.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final SecureRandom secureRandom = new SecureRandom();
    private final long refreshTokenTtlDays;

    // Hash storage by user to avoid storing raw refresh tokens.
    private final Map<UUID, StoredRefreshToken> tokensByUser = new ConcurrentHashMap<>();

    public RefreshTokenServiceImpl(
        @Value("${app.security.jwt.refresh-token-ttl-days:30}") long refreshTokenTtlDays
    ) {
        this.refreshTokenTtlDays = refreshTokenTtlDays;
    }

    @Override
    public String issueToken(UUID userId) {
        Objects.requireNonNull(userId, "userId is required");

        String rawToken = generateToken();
        tokensByUser.put(userId, new StoredRefreshToken(hash(rawToken), expiresAt()));
        return rawToken;
    }

    @Override
    public String rotateToken(UUID userId, String refreshToken) {
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(refreshToken, "refreshToken is required");

        if (!isTokenValid(userId, refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        String newToken = generateToken();
        tokensByUser.put(userId, new StoredRefreshToken(hash(newToken), expiresAt()));
        return newToken;
    }

    @Override
    public boolean isTokenValid(UUID userId, String refreshToken) {
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(refreshToken, "refreshToken is required");

        StoredRefreshToken stored = tokensByUser.get(userId);
        if (stored == null) {
            return false;
        }

        if (stored.expiresAt().isBefore(Instant.now())) {
            return false;
        }

        return stored.tokenHash().equals(hash(refreshToken));
    }

    @Override
    public void revokeToken(UUID userId, String refreshToken) {
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(refreshToken, "refreshToken is required");

        if (isTokenValid(userId, refreshToken)) {
            tokensByUser.remove(userId);
        }
    }

    private String generateToken() {
        byte[] tokenBytes = new byte[48];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private Instant expiresAt() {
        return Instant.now().plus(refreshTokenTtlDays, ChronoUnit.DAYS);
    }

    private static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", ex);
        }
    }

    private record StoredRefreshToken(String tokenHash, Instant expiresAt) {
    }
}
