package com.trading.controller;

import com.trading.dto.spotprice.SpotPriceAttemptResponse;
import com.trading.dto.spotprice.SpotPriceFailureResponse;
import com.trading.dto.spotprice.SpotPriceResponse;
import com.trading.service.spotprice.ProviderAttempt;
import com.trading.service.spotprice.SpotPriceResult;
import com.trading.service.spotprice.SpotPriceService;
import com.trading.service.spotprice.SpotPriceUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/prices")
public class SpotPriceController {

    private final SpotPriceService spotPriceService;

    public SpotPriceController(SpotPriceService spotPriceService) {
        this.spotPriceService = spotPriceService;
    }

    @GetMapping("/spot")
    public ResponseEntity<?> getSpotPrice(@RequestParam("symbol") String symbol) {
        try {
            SpotPriceResult result = spotPriceService.resolveSpotPrice(symbol);
            return ResponseEntity.ok(
                new SpotPriceResponse(
                    result.symbol(),
                    result.priceUsd(),
                    result.source(),
                    result.resolvedPair(),
                    result.fetchedAt()
                )
            );
        } catch (SpotPriceUnavailableException ex) {
            List<SpotPriceAttemptResponse> attempts = ex.getAttempts().stream()
                .map(SpotPriceController::toAttemptResponse)
                .toList();
            SpotPriceFailureResponse response = new SpotPriceFailureResponse(
                "SPOT_PRICE_UNAVAILABLE",
                "Spot price unavailable for symbol: " + ex.getSymbol(),
                attempts
            );
            return ResponseEntity.status(HttpStatus.FAILED_DEPENDENCY).body(response);
        }
    }

    private static SpotPriceAttemptResponse toAttemptResponse(ProviderAttempt attempt) {
        return new SpotPriceAttemptResponse(attempt.provider(), attempt.status(), attempt.reason());
    }
}
