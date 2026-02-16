package com.trading.service.auth;

import com.trading.domain.entity.User;
import com.trading.domain.enums.AuthProvider;
import com.trading.domain.repository.UserRepository;
import com.trading.dto.auth.AuthResponse;
import com.trading.dto.auth.OAuthCallbackRequest;
import com.trading.exception.OAuthEmailConflictException;
import com.trading.security.JwtService;
import com.trading.security.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseAuthOAuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;

    private DatabaseAuthOAuthService service;

    @BeforeEach
    void setUp() {
        service = new DatabaseAuthOAuthService(userRepository, jwtService, refreshTokenService);
    }

    @Test
    void callbackCreatesGoogleUserWhenNoExistingRecords() {
        when(userRepository.findByEmailIgnoreCase("newgoogle@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByUsernameIgnoreCase("new_google")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(jwtService.issueAccessToken(any(UUID.class), eq("newgoogle@example.com"))).thenReturn("access-token");
        when(refreshTokenService.issueToken(any(UUID.class))).thenReturn("refresh-token");

        AuthResponse response = service.handleGoogleCallback(
            new OAuthCallbackRequest("newgoogle@example.com", "google-sub-1", "New.Google")
        );

        assertEquals("newgoogle@example.com", response.email());
        assertEquals(AuthProvider.GOOGLE, response.authProvider());
        assertEquals("new_google", response.username());
        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        assertTrue(response.userId() != null);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void callbackUsesExistingGoogleUserByEmail() {
        UUID userId = UUID.randomUUID();
        User linkedUser = googleUser(userId, "linked@example.com", "linkeduser");
        when(userRepository.findByEmailIgnoreCase("linked@example.com")).thenReturn(Optional.of(linkedUser));
        when(jwtService.issueAccessToken(userId, "linked@example.com")).thenReturn("access-token");
        when(refreshTokenService.issueToken(userId)).thenReturn("refresh-token");

        AuthResponse response = service.handleGoogleCallback(
            new OAuthCallbackRequest("linked@example.com", "google-sub-linked", "ignored")
        );

        assertEquals(userId, response.userId());
        assertEquals("linked@example.com", response.email());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void callbackRejectsEmailAlreadyOwnedByLocalUser() {
        when(userRepository.findByEmailIgnoreCase("local@example.com"))
            .thenReturn(Optional.of(localUser(UUID.randomUUID(), "local@example.com", "localuser")));

        assertThrows(
            OAuthEmailConflictException.class,
            () -> service.handleGoogleCallback(
                new OAuthCallbackRequest("local@example.com", "google-sub-2", "anything")
            )
        );

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void callbackRejectsMissingRequiredFields() {
        assertThrows(
            IllegalArgumentException.class,
            () -> service.handleGoogleCallback(new OAuthCallbackRequest(" ", "google-sub-4", "user"))
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> service.handleGoogleCallback(new OAuthCallbackRequest("google@example.com", " ", "user"))
        );
    }

    @Test
    void callbackGeneratesUniqueUsernameWhenBaseAlreadyExists() {
        when(userRepository.findByEmailIgnoreCase("alpha@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByUsernameIgnoreCase("alpha")).thenReturn(true);
        when(userRepository.existsByUsernameIgnoreCase("alpha1")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(jwtService.issueAccessToken(any(UUID.class), eq("alpha@example.com"))).thenReturn("access-token");
        when(refreshTokenService.issueToken(any(UUID.class))).thenReturn("refresh-token");

        AuthResponse response = service.handleGoogleCallback(
            new OAuthCallbackRequest("alpha@example.com", "google-sub-5", " ")
        );

        assertEquals("alpha1", response.username());
    }

    private static User googleUser(UUID userId, String email, String username) {
        User user = new User();
        user.setId(userId);
        user.setEmail(email);
        user.setUsername(username);
        user.setAuthProvider(AuthProvider.GOOGLE);
        user.setEnabled(true);
        return user;
    }

    private static User localUser(UUID userId, String email, String username) {
        User user = new User();
        user.setId(userId);
        user.setEmail(email);
        user.setUsername(username);
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setEnabled(true);
        return user;
    }
}
