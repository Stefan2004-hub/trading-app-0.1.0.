package com.trading.service.lookup;

import com.trading.domain.entity.Asset;
import com.trading.domain.entity.PricePeak;
import com.trading.domain.entity.Transaction;
import com.trading.domain.entity.User;
import com.trading.domain.repository.PricePeakRepository;
import com.trading.dto.lookup.PricePeakResponse;
import com.trading.dto.lookup.UpdatePricePeakRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricePeakServiceImplTest {

    @Mock
    private PricePeakRepository pricePeakRepository;

    @InjectMocks
    private PricePeakServiceImpl pricePeakService;

    private UUID userId;
    private UUID assetId;
    private UUID pricePeakId;
    private PricePeak row;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        assetId = UUID.randomUUID();
        pricePeakId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);

        Asset asset = new Asset();
        asset.setId(assetId);
        asset.setSymbol("MATIC");
        asset.setName("Polygon");

        Transaction tx = new Transaction();
        tx.setId(UUID.randomUUID());

        row = new PricePeak();
        row.setId(pricePeakId);
        row.setUser(user);
        row.setAsset(asset);
        row.setLastBuyTransaction(tx);
        row.setPeakPrice(new BigDecimal("0.884"));
        row.setPeakTimestamp(OffsetDateTime.parse("2026-02-22T00:29:10.940+02:00"));
        row.setActive(Boolean.TRUE);
        row.setCreatedAt(OffsetDateTime.parse("2026-02-22T00:29:10.962+02:00"));
        row.setUpdatedAt(OffsetDateTime.parse("2026-02-22T00:29:10.962+02:00"));
    }

    @Test
    void listReturnsUserRowsWithoutSearch() {
        when(pricePeakRepository.findAllByUser_IdOrderByUpdatedAtDesc(userId)).thenReturn(List.of(row));

        List<PricePeakResponse> response = pricePeakService.list(userId, null);

        assertEquals(1, response.size());
        assertEquals(pricePeakId, response.get(0).id());
        assertEquals("MATIC", response.get(0).assetSymbol());
    }

    @Test
    void listUsesSearchPatternWhenProvided() {
        when(pricePeakRepository.findByUser_IdAndSearch(userId, "%mat%")).thenReturn(List.of(row));

        List<PricePeakResponse> response = pricePeakService.list(userId, "mat");

        assertEquals(1, response.size());
        verify(pricePeakRepository).findByUser_IdAndSearch(userId, "%mat%");
    }

    @Test
    void updateChangesAllowedFields() {
        UpdatePricePeakRequest request = new UpdatePricePeakRequest(
            new BigDecimal("0.7123"),
            OffsetDateTime.parse("2026-02-25T10:15:00Z"),
            Boolean.FALSE
        );
        when(pricePeakRepository.findByIdAndUser_Id(pricePeakId, userId)).thenReturn(Optional.of(row));
        when(pricePeakRepository.save(row)).thenReturn(row);

        PricePeakResponse response = pricePeakService.update(userId, pricePeakId, request);

        assertEquals(0, response.peakPrice().compareTo(new BigDecimal("0.7123")));
        assertEquals(OffsetDateTime.parse("2026-02-25T10:15:00Z"), response.peakTimestamp());
        assertEquals(Boolean.FALSE, response.active());
    }

    @Test
    void updateRejectsNonPositivePeakPrice() {
        UpdatePricePeakRequest request = new UpdatePricePeakRequest(
            BigDecimal.ZERO,
            OffsetDateTime.parse("2026-02-25T10:15:00Z"),
            Boolean.TRUE
        );

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> pricePeakService.update(userId, pricePeakId, request)
        );

        assertEquals("peakPrice must be positive", ex.getMessage());
    }

    @Test
    void deleteRemovesExistingRow() {
        when(pricePeakRepository.findByIdAndUser_Id(pricePeakId, userId)).thenReturn(Optional.of(row));

        pricePeakService.delete(userId, pricePeakId);

        verify(pricePeakRepository).delete(row);
    }
}
