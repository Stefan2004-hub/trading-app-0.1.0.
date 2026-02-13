package com.trading.controller;

import com.trading.dto.auth.AuthResponse;
import com.trading.dto.auth.LoginRequest;
import com.trading.dto.auth.LoginResponse;
import com.trading.dto.auth.LogoutRequest;
import com.trading.dto.auth.MeResponse;
import com.trading.dto.auth.OAuthCallbackRequest;
import com.trading.dto.auth.RefreshRequest;
import com.trading.dto.auth.RefreshResponse;
import com.trading.dto.auth.RegisterRequest;
import com.trading.dto.auth.RegisterResponse;
import com.trading.security.RefreshTokenService;
import com.trading.security.UserPrincipal;
import com.trading.service.auth.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(AuthService authService, RefreshTokenService refreshTokenService) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse login = authService.login(request);
        String refreshToken = refreshTokenService.issueToken(login.userId());
        RefreshResponse refreshResponse = authService.refresh(login.userId(), new RefreshRequest(refreshToken));
        return ResponseEntity.ok(
            new AuthResponse(
                login.userId(),
                login.email(),
                login.username(),
                login.authProvider(),
                refreshResponse.accessToken(),
                refreshResponse.refreshToken()
            )
        );
    }

    @GetMapping("/oauth2/google")
    public ResponseEntity<Map<String, String>> googleOauthEntrypoint() {
        return ResponseEntity.ok(Map.of("authorizationUrl", "/api/auth/oauth2/callback"));
    }

    @GetMapping("/oauth2/callback")
    public ResponseEntity<AuthResponse> googleOauthCallback(
        @RequestParam String email,
        @RequestParam String providerUserId,
        @RequestParam(required = false) String preferredUsername
    ) {
        AuthResponse response = authService.handleGoogleCallback(
            new OAuthCallbackRequest(email, providerUserId, preferredUsername)
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request, Authentication authentication) {
        UUID userId = extractUserId(authentication);
        return ResponseEntity.ok(authService.refresh(userId, request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request, Authentication authentication) {
        UUID userId = extractUserId(authentication);
        authService.logout(userId, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        return ResponseEntity.ok(authService.me(userId));
    }

    private static UUID extractUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserPrincipal userPrincipal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return userPrincipal.getUserId();
    }
}
