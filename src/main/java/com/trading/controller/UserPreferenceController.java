package com.trading.controller;

import com.trading.dto.user.UpdateUserPreferenceRequest;
import com.trading.dto.user.UserPreferenceResponse;
import com.trading.security.CurrentUserProvider;
import com.trading.service.user.UserPreferenceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/user-preferences")
public class UserPreferenceController {

    private final UserPreferenceService userPreferenceService;
    private final CurrentUserProvider currentUserProvider;

    public UserPreferenceController(UserPreferenceService userPreferenceService, CurrentUserProvider currentUserProvider) {
        this.userPreferenceService = userPreferenceService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public ResponseEntity<UserPreferenceResponse> get() {
        UUID userId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(userPreferenceService.get(userId));
    }

    @PutMapping
    public ResponseEntity<UserPreferenceResponse> update(@Valid @RequestBody UpdateUserPreferenceRequest request) {
        UUID userId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(
            userPreferenceService.updateDefaultBuyInputMode(userId, request.defaultBuyInputMode())
        );
    }
}
