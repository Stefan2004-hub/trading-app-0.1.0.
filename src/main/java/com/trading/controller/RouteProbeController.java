package com.trading.controller;

import com.trading.security.CurrentUserProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RouteProbeController {

    private final CurrentUserProvider currentUserProvider;

    public RouteProbeController(CurrentUserProvider currentUserProvider) {
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/api/auth/ping")
    public ResponseEntity<Map<String, String>> authPing() {
        return ResponseEntity.ok(Map.of("status", "public-ok"));
    }

    @GetMapping("/api/transactions/ping")
    public ResponseEntity<Map<String, String>> transactionsPing() {
        return ResponseEntity.ok(Map.of("status", "protected-ok"));
    }

    @GetMapping("/api/transactions/current-user")
    public ResponseEntity<Map<String, String>> currentUser() {
        return ResponseEntity.ok(Map.of("userId", currentUserProvider.getCurrentUserId().toString()));
    }
}
