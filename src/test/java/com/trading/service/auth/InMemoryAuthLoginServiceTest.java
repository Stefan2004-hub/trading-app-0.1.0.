package com.trading.service.auth;

import com.trading.dto.auth.LoginRequest;
import com.trading.dto.auth.LoginResponse;
import com.trading.dto.auth.RegisterRequest;
import com.trading.exception.InvalidCredentialsException;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryAuthLoginServiceTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final InMemoryAuthRegistrationService registrationService = new InMemoryAuthRegistrationService(passwordEncoder);
    private final InMemoryAuthLoginService loginService = new InMemoryAuthLoginService(registrationService, passwordEncoder);

    @Test
    void loginWithEmailIdentifierSucceeds() {
        registrationService.register(new RegisterRequest("trader@example.com", "satoshi", "Secret123!"));

        LoginResponse response = loginService.login(new LoginRequest("trader@example.com", "Secret123!"));

        assertEquals("trader@example.com", response.email());
        assertEquals("satoshi", response.username());
    }

    @Test
    void loginWithUsernameIdentifierSucceeds() {
        registrationService.register(new RegisterRequest("trader@example.com", "satoshi", "Secret123!"));

        LoginResponse response = loginService.login(new LoginRequest("satoshi", "Secret123!"));

        assertEquals("trader@example.com", response.email());
        assertEquals("satoshi", response.username());
    }

    @Test
    void wrongPasswordIsRejected() {
        registrationService.register(new RegisterRequest("trader@example.com", "satoshi", "Secret123!"));

        assertThrows(
            InvalidCredentialsException.class,
            () -> loginService.login(new LoginRequest("satoshi", "WrongPass1!"))
        );
    }

    @Test
    void unknownIdentifierIsRejected() {
        registrationService.register(new RegisterRequest("trader@example.com", "satoshi", "Secret123!"));

        assertThrows(
            InvalidCredentialsException.class,
            () -> loginService.login(new LoginRequest("unknown", "Secret123!"))
        );
    }

    @Test
    void blankIdentifierOrPasswordIsRejected() {
        registrationService.register(new RegisterRequest("trader@example.com", "satoshi", "Secret123!"));

        assertThrows(
            InvalidCredentialsException.class,
            () -> loginService.login(new LoginRequest("   ", "Secret123!"))
        );
        assertThrows(
            InvalidCredentialsException.class,
            () -> loginService.login(new LoginRequest("satoshi", "   "))
        );
    }

    @Test
    void identifierMatchingIsTrimmedAndCaseInsensitive() {
        registrationService.register(new RegisterRequest("trader@example.com", "satoshi", "Secret123!"));

        LoginResponse response = loginService.login(new LoginRequest("  TRADER@EXAMPLE.COM  ", "Secret123!"));

        assertEquals("trader@example.com", response.email());
        assertEquals("satoshi", response.username());
    }
}
