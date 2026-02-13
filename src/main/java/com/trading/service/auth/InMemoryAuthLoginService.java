package com.trading.service.auth;

import com.trading.dto.auth.LoginRequest;
import com.trading.dto.auth.LoginResponse;
import com.trading.exception.InvalidCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class InMemoryAuthLoginService implements AuthLoginService {

    private final InMemoryAuthRegistrationService registrationService;
    private final PasswordEncoder passwordEncoder;

    public InMemoryAuthLoginService(
        InMemoryAuthRegistrationService registrationService,
        PasswordEncoder passwordEncoder
    ) {
        this.registrationService = registrationService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        Objects.requireNonNull(request, "request is required");
        if (isBlank(request.identifier()) || isBlank(request.password())) {
            throw new InvalidCredentialsException();
        }

        InMemoryAuthRegistrationService.RegisteredUser user =
            registrationService.findByIdentifier(request.identifier());

        if (user == null || !passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new InvalidCredentialsException();
        }

        return new LoginResponse(user.id(), user.email(), user.username(), user.authProvider());
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
