package com.trading.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RouteProbeController {

    @GetMapping("/api/auth/ping")
    public ResponseEntity<Map<String, String>> authPing() {
        return ResponseEntity.ok(Map.of("status", "public-ok"));
    }

    @GetMapping("/api/transactions/ping")
    public ResponseEntity<Map<String, String>> transactionsPing() {
        return ResponseEntity.ok(Map.of("status", "protected-ok"));
    }
}
