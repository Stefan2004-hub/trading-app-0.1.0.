package com.trading.service.transaction;

import com.trading.domain.entity.AccumulationTrade;
import com.trading.domain.entity.Asset;
import com.trading.domain.entity.Transaction;
import com.trading.domain.entity.User;
import com.trading.domain.enums.AccumulationTradeStatus;
import com.trading.domain.enums.TransactionType;
import com.trading.domain.repository.AccumulationTradeRepository;
import com.trading.domain.repository.TransactionRepository;
import com.trading.dto.transaction.AccumulationTradeResponse;
import com.trading.dto.transaction.OpenAccumulationTradeRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
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
    private User user;
    private Asset asset;
    private Transaction sellTransaction;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        assetId = UUID.randomUUID();
        exitTxId = UUID.randomUUID();

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

        lenient().when(transactionRepository.findByIdAndUser_Id(exitTxId, userId))
            .thenReturn(Optional.of(sellTransaction));
        lenient().when(accumulationTradeRepository.findByUser_IdAndExitTransaction_Id(userId, exitTxId))
            .thenReturn(Optional.empty());
        lenient().when(accumulationTradeRepository.save(any(AccumulationTrade.class))).thenAnswer(invocation -> {
            AccumulationTrade trade = invocation.getArgument(0, AccumulationTrade.class);
            trade.setId(UUID.randomUUID());
            return trade;
        });
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
}
