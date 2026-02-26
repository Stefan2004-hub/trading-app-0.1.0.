package com.trading.controller;

import com.trading.service.system.SystemMaintenanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminMaintenanceController {

    private final SystemMaintenanceService systemMaintenanceService;

    public AdminMaintenanceController(SystemMaintenanceService systemMaintenanceService) {
        this.systemMaintenanceService = systemMaintenanceService;
    }

    @GetMapping("/ping-status")
    public ResponseEntity<Map<String, Boolean>> pingStatus() {
        return ResponseEntity.ok(Map.of("keepAliveActive", systemMaintenanceService.isKeepAliveActive()));
    }

    @PostMapping("/toggle-ping")
    public ResponseEntity<Map<String, Boolean>> togglePing() {
        return ResponseEntity.ok(Map.of("keepAliveActive", systemMaintenanceService.toggleKeepAlive()));
    }
}
