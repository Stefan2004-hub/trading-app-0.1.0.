package com.trading.controller;

import com.trading.dto.lookup.PricePeakResponse;
import com.trading.dto.lookup.UpdatePricePeakRequest;
import com.trading.security.CurrentUserProvider;
import com.trading.service.lookup.PricePeakService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/price-peaks")
public class PricePeakController {

    private final PricePeakService pricePeakService;
    private final CurrentUserProvider currentUserProvider;

    public PricePeakController(
        PricePeakService pricePeakService,
        CurrentUserProvider currentUserProvider
    ) {
        this.pricePeakService = pricePeakService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public ResponseEntity<List<PricePeakResponse>> list(
        @RequestParam(name = "search", required = false) String search
    ) {
        UUID userId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(pricePeakService.list(userId, search));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PricePeakResponse> update(
        @PathVariable("id") UUID pricePeakId,
        @Valid @RequestBody UpdatePricePeakRequest request
    ) {
        UUID userId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(pricePeakService.update(userId, pricePeakId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID pricePeakId) {
        UUID userId = currentUserProvider.getCurrentUserId();
        pricePeakService.delete(userId, pricePeakId);
        return ResponseEntity.noContent().build();
    }
}
