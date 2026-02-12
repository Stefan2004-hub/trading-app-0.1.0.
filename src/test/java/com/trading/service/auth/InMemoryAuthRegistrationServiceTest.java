package com.trading.service.auth;

import com.trading.domain.enums.AuthProvider;
import com.trading.dto.auth.RegisterRequest;
import com.trading.dto.auth.RegisterResponse;
import com.trading.exception.DuplicateEmailException;
import com.trading.exception.DuplicateUsernameException;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryAuthRegistrationServiceTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final InMemoryAuthRegistrationService service = new InMemoryAuthRegistrationService(passwordEncoder);

    @Test
    void registerSucceedsAndStoresBcryptHash() {
        RegisterRequest request = new RegisterRequest("trader@example.com", "satoshi", "Secret123!");

        RegisterResponse response = service.register(request);
        InMemoryAuthRegistrationService.RegisteredUser storedUser = service.findByEmail("trader@example.com");

        assertNotNull(response.userId());
        assertEquals("trader@example.com", response.email());
        assertEquals("satoshi", response.username());
        assertEquals(AuthProvider.LOCAL, response.authProvider());

        assertNotNull(storedUser);
        assertNotEquals("Secret123!", storedUser.passwordHash());
        assertTrue(passwordEncoder.matches("Secret123!", storedUser.passwordHash()));
    }

    @Test
    void duplicateEmailIsRejected() {
        service.register(new RegisterRequest("trader@example.com", "satoshi", "Secret123!"));

        assertThrows(
            DuplicateEmailException.class,
            () -> service.register(new RegisterRequest("TRADER@example.com", "hal", "AnotherPass1!"))
        );
    }

    @Test
    void duplicateUsernameIsRejected() {
        service.register(new RegisterRequest("trader@example.com", "satoshi", "Secret123!"));

        assertThrows(
            DuplicateUsernameException.class,
            () -> service.register(new RegisterRequest("another@example.com", "SATOSHI", "AnotherPass1!"))
        );
    }
}
