package com.trading.service.auth;

import com.trading.domain.enums.AuthProvider;
import com.trading.dto.auth.RegisterRequest;
import com.trading.dto.auth.RegisterResponse;
import com.trading.exception.DuplicateEmailException;
import com.trading.exception.DuplicateUsernameException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryAuthRegistrationService implements AuthRegistrationService {

    private final PasswordEncoder passwordEncoder;
    private final Map<String, RegisteredUser> usersByEmail = new ConcurrentHashMap<>();
    private final Map<String, RegisteredUser> usersByUsername = new ConcurrentHashMap<>();

    public InMemoryAuthRegistrationService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public RegisterResponse register(RegisterRequest request) {
        validateRequest(request);

        String normalizedEmail = normalizeEmail(request.email());
        String normalizedUsername = normalizeUsername(request.username());

        if (usersByEmail.containsKey(normalizedEmail)) {
            throw new DuplicateEmailException(request.email());
        }
        if (usersByUsername.containsKey(normalizedUsername)) {
            throw new DuplicateUsernameException(request.username());
        }

        RegisteredUser user = new RegisteredUser(
            UUID.randomUUID(),
            request.email(),
            request.username(),
            passwordEncoder.encode(request.password()),
            AuthProvider.LOCAL
        );

        usersByEmail.put(normalizedEmail, user);
        usersByUsername.put(normalizedUsername, user);

        return new RegisterResponse(user.id(), user.email(), user.username(), user.authProvider());
    }

    RegisteredUser findByEmail(String email) {
        return usersByEmail.get(normalizeEmail(email));
    }

    RegisteredUser findByIdentifier(String identifier) {
        if (isBlank(identifier)) {
            return null;
        }

        String normalized = identifier.trim().toLowerCase(Locale.ROOT);
        RegisteredUser byEmail = usersByEmail.get(normalized);
        if (byEmail != null) {
            return byEmail;
        }
        return usersByUsername.get(normalized);
    }

    synchronized RegisteredUser getOrCreateGoogleUser(String email, String preferredUsername) {
        if (isBlank(email)) {
            throw new IllegalArgumentException("email is required");
        }

        RegisteredUser existing = usersByEmail.get(normalizeEmail(email));
        if (existing != null) {
            return existing;
        }

        String rawUsername = isBlank(preferredUsername) ? defaultUsernameFromEmail(email) : preferredUsername;
        String baseUsername = normalizeUsername(rawUsername);
        String uniqueUsername = ensureUniqueUsername(baseUsername);

        RegisteredUser created = new RegisteredUser(
            UUID.randomUUID(),
            email.trim(),
            uniqueUsername,
            null,
            AuthProvider.GOOGLE
        );

        usersByEmail.put(normalizeEmail(email), created);
        usersByUsername.put(uniqueUsername, created);
        return created;
    }

    private static void validateRequest(RegisterRequest request) {
        Objects.requireNonNull(request, "request is required");
        if (isBlank(request.email())) {
            throw new IllegalArgumentException("email is required");
        }
        if (isBlank(request.username())) {
            throw new IllegalArgumentException("username is required");
        }
        if (isBlank(request.password())) {
            throw new IllegalArgumentException("password is required");
        }
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String ensureUniqueUsername(String baseUsername) {
        String candidate = baseUsername;
        int suffix = 1;
        while (usersByUsername.containsKey(candidate)) {
            candidate = baseUsername + suffix;
            suffix++;
        }
        return candidate;
    }

    private static String defaultUsernameFromEmail(String email) {
        String localPart = email.split("@")[0].trim();
        return isBlank(localPart) ? "google_user" : localPart;
    }

    record RegisteredUser(
        UUID id,
        String email,
        String username,
        String passwordHash,
        AuthProvider authProvider
    ) {
    }
}
