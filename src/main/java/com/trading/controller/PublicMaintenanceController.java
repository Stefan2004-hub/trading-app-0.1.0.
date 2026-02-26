package com.trading.controller;

import com.trading.service.system.SystemMaintenanceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class PublicMaintenanceController {

    private final SystemMaintenanceService systemMaintenanceService;

    public PublicMaintenanceController(SystemMaintenanceService systemMaintenanceService) {
        this.systemMaintenanceService = systemMaintenanceService;
    }

    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        boolean keepAliveActive = systemMaintenanceService.isKeepAliveActive();
        if (keepAliveActive) {
            return ResponseEntity.ok(Map.of("keepAliveActive", true, "status", "UP"));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of("keepAliveActive", false, "status", "SLEEP"));
    }
}
