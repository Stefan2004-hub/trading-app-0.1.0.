package com.trading.controller;

import com.trading.dto.portfolio.PortfolioAssetPerformanceResponse;
import com.trading.dto.portfolio.PortfolioSummaryResponse;
import com.trading.security.CurrentUserProvider;
import com.trading.service.portfolio.PortfolioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final CurrentUserProvider currentUserProvider;

    public PortfolioController(
        PortfolioService portfolioService,
        CurrentUserProvider currentUserProvider
    ) {
        this.portfolioService = portfolioService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/summary")
    public ResponseEntity<PortfolioSummaryResponse> summary() {
        UUID userId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(portfolioService.getSummary(userId));
    }

    @GetMapping("/performance")
    public ResponseEntity<List<PortfolioAssetPerformanceResponse>> performance() {
        UUID userId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(portfolioService.getPerformance(userId));
    }
}
