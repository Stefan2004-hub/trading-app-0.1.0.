package com.trading.controller;

import com.trading.dto.marketalert.MarketAlertResponse;
import com.trading.dto.marketalert.MarketScanResponse;
import com.trading.dto.marketalert.MarketSnapshotDTO;
import com.trading.security.CurrentUserProvider;
import com.trading.service.marketalert.MarketAlertService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketAlertControllerTest {

    @Mock
    private MarketAlertService marketAlertService;
    @Mock
    private CurrentUserProvider currentUserProvider;

    private MarketAlertController controller;

    @BeforeEach
    void setUp() {
        controller = new MarketAlertController(marketAlertService, currentUserProvider);
    }

    @Test
    void listSummaryReturnsServiceData() {
        List<MarketSnapshotDTO> snapshots = List.of(
            new MarketSnapshotDTO("Bitcoin", "BTC", new BigDecimal("28.1234"), new BigDecimal("19.1000"), LocalDate.of(2026, 2, 22), "UP")
        );
        when(marketAlertService.listTechnicalSummary()).thenReturn(snapshots);

        List<MarketSnapshotDTO> response = controller.listSummary().getBody();

        assertEquals(snapshots, response);
        verify(marketAlertService).listTechnicalSummary();
    }

    @Test
    void scanDelegatesAndReturnsSnapshotsCount() {
        UUID userId = UUID.randomUUID();
        MarketScanResponse scanResponse = new MarketScanResponse(5, 2, 1, 1, 4);
        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(marketAlertService.scan(userId)).thenReturn(scanResponse);

        MarketScanResponse response = controller.scan().getBody();

        assertEquals(4, response.snapshotsUpdated());
        verify(marketAlertService).scan(userId);
    }

    @Test
    void listDelegatesToService() {
        UUID userId = UUID.randomUUID();
        List<MarketAlertResponse> alerts = List.of();
        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(marketAlertService.list(userId)).thenReturn(alerts);

        List<MarketAlertResponse> response = controller.list().getBody();

        assertEquals(alerts, response);
        verify(marketAlertService).list(userId);
    }
}
