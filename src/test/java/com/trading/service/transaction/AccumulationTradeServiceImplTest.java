package com.trading.service.transaction;

import com.trading.domain.entity.AccumulationTrade;
import com.trading.domain.entity.Asset;
import com.trading.domain.entity.Transaction;
import com.trading.domain.entity.User;
import com.trading.domain.enums.AccumulationTradeStatus;
import com.trading.domain.enums.TransactionType;
import com.trading.domain.projection.AccumulationTradeAssetSummaryProjection;
import com.trading.domain.repository.AccumulationTradeRepository;
import com.trading.domain.repository.TransactionRepository;
import com.trading.dto.transaction.AccumulationTradeAssetSummaryResponse;
import com.trading.dto.transaction.AccumulationTradeResponse;
import com.trading.dto.transaction.CloseAccumulationTradeRequest;
import com.trading.dto.transaction.OpenAccumulationTradeRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccumulationTradeServiceImplTest {

    @Mock
    private AccumulationTradeRepository accumulationTradeRepository;
    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccumulationTradeServiceImpl accumulationTradeService;

    private UUID userId;
    private UUID assetId;
    private UUID exitTxId;
    private UUID reentryTxId;
    private UUID tradeId;
    private User user;
    private Asset asset;
    private Transaction sellTransaction;
    private Transaction buyTransaction;
    private AccumulationTrade openTrade;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        assetId = UUID.randomUUID();
        exitTxId = UUID.randomUUID();
        reentryTxId = UUID.randomUUID();
        tradeId = UUID.randomUUID();

        user = new User();
        user.setId(userId);

        asset = new Asset();
        asset.setId(assetId);
        asset.setSymbol("BTC");

        sellTransaction = new Transaction();
        sellTransaction.setId(exitTxId);
        sellTransaction.setUser(user);
        sellTransaction.setAsset(asset);
        sellTransaction.setTransactionType(TransactionType.SELL);
        sellTransaction.setGrossAmount(new BigDecimal("1.250000000000000000"));
        sellTransaction.setUnitPriceUsd(new BigDecimal("60000.000000000000000000"));
        sellTransaction.setTransactionDate(OffsetDateTime.parse("2026-02-13T11:00:00Z"));

        buyTransaction = new Transaction();
        buyTransaction.setId(reentryTxId);
        buyTransaction.setUser(user);
        buyTransaction.setAsset(asset);
        buyTransaction.setTransactionType(TransactionType.BUY);
        buyTransaction.setNetAmount(new BigDecimal("1.300000000000000000"));
        buyTransaction.setUnitPriceUsd(new BigDecimal("55000.000000000000000000"));
        buyTransaction.setTransactionDate(OffsetDateTime.parse("2026-02-14T11:00:00Z"));

        openTrade = new AccumulationTrade();
        openTrade.setId(tradeId);
        openTrade.setUser(user);
        openTrade.setAsset(asset);
        openTrade.setExitTransaction(sellTransaction);
        openTrade.setOldCoinAmount(new BigDecimal("1.250000000000000000"));
        openTrade.setStatus(AccumulationTradeStatus.OPEN);
        openTrade.setExitPriceUsd(new BigDecimal("60000.000000000000000000"));
        openTrade.setCreatedAt(OffsetDateTime.parse("2026-02-13T11:01:00Z"));

        lenient().when(transactionRepository.findByIdAndUser_Id(exitTxId, userId))
            .thenReturn(Optional.of(sellTransaction));
        lenient().when(transactionRepository.findByIdAndUser_Id(reentryTxId, userId))
            .thenReturn(Optional.of(buyTransaction));
        lenient().when(accumulationTradeRepository.findByIdAndUser_Id(tradeId, userId))
            .thenReturn(Optional.of(openTrade));
        lenient().when(accumulationTradeRepository.findByUser_IdAndExitTransaction_Id(userId, exitTxId))
            .thenReturn(Optional.empty());
        lenient().when(accumulationTradeRepository.save(any(AccumulationTrade.class))).thenAnswer(invocation -> {
            AccumulationTrade trade = invocation.getArgument(0, AccumulationTrade.class);
            if (trade.getId() == null) {
                trade.setId(UUID.randomUUID());
            }
            return trade;
        });
    }

    @Test
    void listReturnsPagedResponsesFilteredByStatusAndAsset() {
        UUID requestedAssetId = assetId;
        PageRequest expectedPageRequest = PageRequest.of(1, 10, Sort.by(Sort.Order.desc("createdAt")));
        when(accumulationTradeRepository.findAllByUser_IdAndStatusAndAsset_Id(
            userId,
            AccumulationTradeStatus.CLOSED,
            requestedAssetId,
            expectedPageRequest
        )).thenReturn(new PageImpl<>(List.of(openTrade), expectedPageRequest, 11));

        Page<AccumulationTradeResponse> response = accumulationTradeService.list(
            userId,
            1,
            10,
            AccumulationTradeStatus.CLOSED,
            requestedAssetId
        );

        assertEquals(11, response.getTotalElements());
        assertEquals(tradeId, response.getContent().get(0).id());
        verify(accumulationTradeRepository).findAllByUser_IdAndStatusAndAsset_Id(
            eq(userId),
            eq(AccumulationTradeStatus.CLOSED),
            eq(requestedAssetId),
            eq(expectedPageRequest)
        );
    }

    @Test
    void listReturnsPagedResponsesForUserOnly() {
        PageRequest expectedPageRequest = PageRequest.of(0, 20, Sort.by(Sort.Order.desc("createdAt")));
        when(accumulationTradeRepository.findAllByUser_Id(userId, expectedPageRequest))
            .thenReturn(new PageImpl<>(List.of(openTrade), expectedPageRequest, 1));

        Page<AccumulationTradeResponse> response = accumulationTradeService.list(userId, 0, 20, null, null);

        assertEquals(1, response.getTotalElements());
        assertEquals(tradeId, response.getContent().get(0).id());
        verify(accumulationTradeRepository).findAllByUser_Id(eq(userId), eq(expectedPageRequest));
    }

    @Test
    void summarizeByAssetDefaultsToClosedStatus() {
        AccumulationTradeAssetSummaryProjection projection = new TestAccumulationTradeAssetSummaryProjection(
            assetId,
            new BigDecimal("0.125000000000000000"),
            3
        );
        when(accumulationTradeRepository.summarizeByAssetAndStatus(userId, AccumulationTradeStatus.CLOSED))
            .thenReturn(List.of(projection));

        List<AccumulationTradeAssetSummaryResponse> response = accumulationTradeService.summarizeByAsset(
            userId,
            null,
            null
        );

        assertEquals(1, response.size());
        assertEquals(assetId, response.get(0).assetId());
        assertEquals(0, response.get(0).totalAccumulationDelta().compareTo(new BigDecimal("0.125")));
        assertEquals(3, response.get(0).tradeCount());
        verify(accumulationTradeRepository).summarizeByAssetAndStatus(userId, AccumulationTradeStatus.CLOSED);
    }

    @Test
    void summarizeByAssetUsesAssetSpecificQueryWhenAssetIdProvided() {
        AccumulationTradeAssetSummaryProjection projection = new TestAccumulationTradeAssetSummaryProjection(
            assetId,
            new BigDecimal("0.200000000000000000"),
            2
        );
        when(accumulationTradeRepository.summarizeByAssetAndStatusAndAssetId(
            userId,
            AccumulationTradeStatus.CLOSED,
            assetId
        )).thenReturn(List.of(projection));

        List<AccumulationTradeAssetSummaryResponse> response = accumulationTradeService.summarizeByAsset(
            userId,
            null,
            assetId
        );

        assertEquals(1, response.size());
        assertEquals(assetId, response.get(0).assetId());
        assertEquals(0, response.get(0).totalAccumulationDelta().compareTo(new BigDecimal("0.2")));
        assertEquals(2, response.get(0).tradeCount());
        verify(accumulationTradeRepository).summarizeByAssetAndStatusAndAssetId(
            userId,
            AccumulationTradeStatus.CLOSED,
            assetId
        );
    }

    @Test
    void openPersistsTradeLinkedToSellTransaction() {
        OpenAccumulationTradeRequest request = new OpenAccumulationTradeRequest(exitTxId, "waiting for dip");

        AccumulationTradeResponse response = accumulationTradeService.open(userId, request);

        assertEquals(userId, response.userId());
        assertEquals(assetId, response.assetId());
        assertEquals(exitTxId, response.exitTransactionId());
        assertEquals(0, response.oldCoinAmount().compareTo(new BigDecimal("1.25")));
        assertEquals(0, response.exitPriceUsd().compareTo(new BigDecimal("60000")));
        assertEquals(AccumulationTradeStatus.OPEN, response.status());
        assertEquals("waiting for dip", response.predictionNotes());
    }

    @Test
    void openRejectsWhenExitTransactionIsNotSell() {
        Transaction buyTx = new Transaction();
        buyTx.setId(exitTxId);
        buyTx.setUser(user);
        buyTx.setAsset(asset);
        buyTx.setTransactionType(TransactionType.BUY);
        buyTx.setGrossAmount(new BigDecimal("1.0"));
        buyTx.setUnitPriceUsd(new BigDecimal("50000"));

        when(transactionRepository.findByIdAndUser_Id(exitTxId, userId))
            .thenReturn(Optional.of(buyTx));

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> accumulationTradeService.open(userId, new OpenAccumulationTradeRequest(exitTxId, null))
        );

        assertEquals("Exit transaction must be SELL", ex.getMessage());
    }

    @Test
    void openRejectsWhenTradeAlreadyExistsForExitTransaction() {
        AccumulationTrade existing = new AccumulationTrade();
        existing.setId(UUID.randomUUID());
        existing.setStatus(AccumulationTradeStatus.OPEN);

        when(accumulationTradeRepository.findByUser_IdAndExitTransaction_Id(userId, exitTxId))
            .thenReturn(Optional.of(existing));

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> accumulationTradeService.open(userId, new OpenAccumulationTradeRequest(exitTxId, null))
        );

        assertEquals("Accumulation trade already exists for this exit transaction", ex.getMessage());
    }

    @Test
    void closeSetsReentryAndStatusAndDelta() {
        CloseAccumulationTradeRequest request = new CloseAccumulationTradeRequest(
            tradeId,
            reentryTxId,
            "reentered on dip"
        );

        AccumulationTradeResponse response = accumulationTradeService.close(userId, request);

        assertEquals(AccumulationTradeStatus.CLOSED, response.status());
        assertEquals(reentryTxId, response.reentryTransactionId());
        assertEquals(0, response.newCoinAmount().compareTo(new BigDecimal("1.3")));
        assertEquals(0, response.accumulationDelta().compareTo(new BigDecimal("0.05")));
        assertEquals("reentered on dip", response.predictionNotes());
    }

    @Test
    void closeRejectsWhenReentryTransactionIsNotBuy() {
        Transaction nonBuy = new Transaction();
        nonBuy.setId(reentryTxId);
        nonBuy.setUser(user);
        nonBuy.setAsset(asset);
        nonBuy.setTransactionType(TransactionType.SELL);

        when(transactionRepository.findByIdAndUser_Id(reentryTxId, userId))
            .thenReturn(Optional.of(nonBuy));

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> accumulationTradeService.close(
                userId,
                new CloseAccumulationTradeRequest(tradeId, reentryTxId, null)
            )
        );

        assertEquals("Reentry transaction must be BUY", ex.getMessage());
    }

    @Test
    void closeRejectsWhenTradeIsNotOpen() {
        openTrade.setStatus(AccumulationTradeStatus.CLOSED);

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> accumulationTradeService.close(
                userId,
                new CloseAccumulationTradeRequest(tradeId, reentryTxId, null)
            )
        );

        assertEquals("Only OPEN accumulation trades can be closed", ex.getMessage());
    }

    private record TestAccumulationTradeAssetSummaryProjection(
        UUID assetId,
        BigDecimal totalAccumulationDelta,
        long tradeCount
    ) implements AccumulationTradeAssetSummaryProjection {

        @Override
        public UUID getAssetId() {
            return assetId;
        }

        @Override
        public BigDecimal getTotalAccumulationDelta() {
            return totalAccumulationDelta;
        }

        @Override
        public long getTradeCount() {
            return tradeCount;
        }
    }
}
