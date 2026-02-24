package com.trading.controller;

import com.trading.service.spotprice.ProviderAttempt;
import com.trading.service.spotprice.SpotPriceResult;
import com.trading.service.spotprice.SpotPriceService;
import com.trading.service.spotprice.SpotPriceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpotPriceControllerTest {

    @Mock
    private SpotPriceService spotPriceService;

    private SpotPriceController controller;

    @BeforeEach
    void setUp() {
        controller = new SpotPriceController(spotPriceService);
    }

    @Test
    void getSpotPriceReturnsSuccessPayload() {
        OffsetDateTime fetchedAt = OffsetDateTime.parse("2026-02-23T12:00:00Z");
        when(spotPriceService.resolveSpotPrice("ORDER"))
            .thenReturn(new SpotPriceResult("ORDER", "0.1234", "gateio", "ORDER_USDT", fetchedAt));

        ResponseEntity<?> response = controller.getSpotPrice("ORDER");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getSpotPriceReturnsFailedDependencyWhenAllProvidersFail() {
        when(spotPriceService.resolveSpotPrice("ORDER"))
            .thenThrow(
                new SpotPriceUnavailableException(
                    "ORDER",
                    List.of(
                        new ProviderAttempt("coinbase", 404, "coinbase returned 404"),
                        new ProviderAttempt("gateio", 404, "gateio returned 404")
                    )
                )
            );

        ResponseEntity<?> response = controller.getSpotPrice("ORDER");

        assertEquals(HttpStatus.FAILED_DEPENDENCY, response.getStatusCode());
    }
}
