package com.trading.controller;

import com.trading.dto.historical.HistoricalDataRowResponse;
import com.trading.dto.historical.HistoricalDataSyncResponse;
import com.trading.dto.historical.HistoricalAssetRefreshStatusResponse;
import com.trading.dto.historical.HistoricalSyncStartResponse;
import com.trading.security.CurrentUserProvider;
import com.trading.service.historical.HistoricalDataService;
import org.springframework.http.HttpStatusCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoricalDataControllerTest {

    @Mock
    private HistoricalDataService historicalDataService;
    @Mock
    private CurrentUserProvider currentUserProvider;

    private HistoricalDataController historicalDataController;

    @BeforeEach
    void setUp() {
        historicalDataController = new HistoricalDataController(historicalDataService, currentUserProvider);
    }

    @Test
    void listDelegatesToPagedServiceWithExplicitParams() {
        UUID userId = UUID.randomUUID();
        Page<HistoricalDataRowResponse> page = new PageImpl<>(
            List.of(),
            PageRequest.of(1, 50),
            0
        );
        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(historicalDataService.listForUser(userId, 1, 50)).thenReturn(page);

        Page<HistoricalDataRowResponse> response = historicalDataController.list(1, 50).getBody();

        assertEquals(page, response);
        verify(historicalDataService).listForUser(userId, 1, 50);
    }

    @Test
    void refreshWithoutAssetIdStartsAsyncGlobalSync() {
        UUID userId = UUID.randomUUID();
        HistoricalSyncStartResponse startResponse = new HistoricalSyncStartResponse("STARTED", "Sync Started", OffsetDateTime.now());
        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(historicalDataService.startExtremeSync(userId)).thenReturn(startResponse);

        var response = historicalDataController.refresh(null);

        assertEquals(HttpStatusCode.valueOf(202), response.getStatusCode());
        assertEquals(startResponse, response.getBody());
        verify(historicalDataService).startExtremeSync(userId);
    }

    @Test
    void refreshWithAssetIdDelegatesSingleAssetSync() {
        UUID userId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        HistoricalDataSyncResponse syncResponse = new HistoricalDataSyncResponse(1, 2, 0, List.of());
        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(historicalDataService.syncIncremental(userId, assetId)).thenReturn(syncResponse);

        Object response = historicalDataController.refresh(assetId).getBody();

        assertEquals(syncResponse, response);
        verify(historicalDataService).syncIncremental(userId, assetId);
    }

    @Test
    void listMissingTodayDelegatesToService() {
        UUID userId = UUID.randomUUID();
        List<HistoricalAssetRefreshStatusResponse> items = List.of(
            new HistoricalAssetRefreshStatusResponse(
                UUID.randomUUID(),
                "BTC",
                "Bitcoin",
                java.time.LocalDate.of(2026, 2, 22)
            )
        );
        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(historicalDataService.listAssetsNeedingRefreshToday(userId)).thenReturn(items);

        List<HistoricalAssetRefreshStatusResponse> response = historicalDataController.listMissingToday().getBody();

        assertEquals(items, response);
        verify(historicalDataService).listAssetsNeedingRefreshToday(userId);
    }
}
