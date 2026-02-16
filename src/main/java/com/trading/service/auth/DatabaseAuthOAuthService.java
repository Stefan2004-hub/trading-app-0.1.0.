package com.trading.service.auth;

import com.trading.domain.entity.User;
import com.trading.domain.enums.AuthProvider;
import com.trading.domain.repository.UserRepository;
import com.trading.dto.auth.AuthResponse;
import com.trading.dto.auth.OAuthCallbackRequest;
import com.trading.exception.OAuthEmailConflictException;
import com.trading.security.JwtService;
import com.trading.security.RefreshTokenService;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@Primary
public class DatabaseAuthOAuthService implements AuthOAuthService {
    private static final Logger log = LoggerFactory.getLogger(DatabaseAuthOAuthService.class);

    private static final String DEFAULT_USERNAME = "google_user";
    private static final int MAX_USERNAME_LENGTH = 50;
    private static final Pattern NON_USERNAME_CHARS = Pattern.compile("[^a-z0-9_]");
    private static final Pattern REPEATED_UNDERSCORES = Pattern.compile("_+");

    @Nullable
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public DatabaseAuthOAuthService(
        @Nullable UserRepository userRepository,
        JwtService jwtService,
        RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    @Transactional
    public AuthResponse handleGoogleCallback(OAuthCallbackRequest request) {
        Objects.requireNonNull(request, "request is required");
        if (userRepository == null) {
            throw new IllegalStateException("UserRepository is required for Google OAuth persistence");
        }

        String normalizedEmail = normalizeEmail(request.email());
        normalizeRequired(request.providerUserId(), "providerUserId");
        User user = upsertGoogleUser(normalizedEmail, request.preferredUsername());
        log.info("Google OAuth handled via DatabaseAuthOAuthService for email={}", normalizedEmail);

        String accessToken = jwtService.issueAccessToken(user.getId(), user.getEmail());
        String refreshToken = refreshTokenService.issueToken(user.getId());

        return new AuthResponse(
            user.getId(),
            user.getEmail(),
            user.getUsername(),
            user.getAuthProvider(),
            accessToken,
            refreshToken
        );
    }

    private User upsertGoogleUser(String normalizedEmail, String preferredUsername) {
        Optional<User> existingByEmail = userRepository.findByEmailIgnoreCase(normalizedEmail);
        if (existingByEmail.isPresent()) {
            User existingUser = existingByEmail.get();
            if (existingUser.getAuthProvider() == AuthProvider.LOCAL) {
                throw new OAuthEmailConflictException();
            }
            return existingUser;
        }

        User created = new User();
        created.setEmail(normalizedEmail);
        created.setUsername(generateUniqueUsername(preferredUsername, normalizedEmail));
        created.setPasswordHash(null);
        created.setAuthProvider(AuthProvider.GOOGLE);
        created.setEnabled(true);

        return userRepository.save(created);
    }

    private String generateUniqueUsername(String preferredUsername, String email) {
        String baseCandidate = isBlank(preferredUsername) ? emailLocalPart(email) : preferredUsername;
        String normalizedBase = normalizeUsername(baseCandidate);

        if (!userRepository.existsByUsernameIgnoreCase(normalizedBase)) {
            return normalizedBase;
        }

        int suffix = 1;
        while (true) {
            String suffixValue = String.valueOf(suffix);
            int baseMaxLength = MAX_USERNAME_LENGTH - suffixValue.length();
            String truncatedBase = truncate(normalizedBase, baseMaxLength);
            String withSuffix = truncatedBase + suffixValue;
            if (!userRepository.existsByUsernameIgnoreCase(withSuffix)) {
                return withSuffix;
            }
            suffix++;
        }
    }

    private static String normalizeEmail(String email) {
        return normalizeRequired(email, "email").toLowerCase(Locale.ROOT);
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeUsername(String value) {
        String lowered = value.trim().toLowerCase(Locale.ROOT);
        String replaced = NON_USERNAME_CHARS.matcher(lowered).replaceAll("_");
        String collapsed = REPEATED_UNDERSCORES.matcher(replaced).replaceAll("_");
        String stripped = stripEdgeUnderscores(collapsed);
        String fallback = stripped.isBlank() ? DEFAULT_USERNAME : stripped;
        return truncate(fallback, MAX_USERNAME_LENGTH);
    }

    private static String stripEdgeUnderscores(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && value.charAt(start) == '_') {
            start++;
        }
        while (end > start && value.charAt(end - 1) == '_') {
            end--;
        }
        return value.substring(start, end);
    }

    private static String emailLocalPart(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return DEFAULT_USERNAME;
        }
        return email.substring(0, atIndex);
    }

    private static String truncate(String value, int maxLength) {
        if (maxLength <= 0) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
