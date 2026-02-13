package com.trading.service.transaction;

import com.trading.domain.entity.Asset;
import com.trading.domain.entity.Exchange;
import com.trading.domain.entity.Transaction;
import com.trading.domain.entity.User;
import com.trading.domain.enums.TransactionType;
import com.trading.domain.repository.AssetRepository;
import com.trading.domain.repository.ExchangeRepository;
import com.trading.domain.repository.TransactionRepository;
import com.trading.domain.repository.UserRepository;
import com.trading.dto.transaction.BuyTransactionRequest;
import com.trading.dto.transaction.SellTransactionRequest;
import com.trading.dto.transaction.TransactionResponse;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AssetRepository assetRepository;
    @Mock
    private ExchangeRepository exchangeRepository;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private UUID userId;
    private UUID assetId;
    private UUID exchangeId;
    private User user;
    private Asset asset;
    private Exchange exchange;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        assetId = UUID.randomUUID();
        exchangeId = UUID.randomUUID();

        user = new User();
        user.setId(userId);

        asset = new Asset();
        asset.setId(assetId);
        asset.setSymbol("BTC");

        exchange = new Exchange();
        exchange.setId(exchangeId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(exchangeRepository.findById(exchangeId)).thenReturn(Optional.of(exchange));
        lenient().when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction tx = invocation.getArgument(0, Transaction.class);
            tx.setId(UUID.randomUUID());
            return tx;
        });
    }

    @Test
    void buyWithoutFeeKeepsNetEqualGross() {
        BuyTransactionRequest request = new BuyTransactionRequest(
            assetId,
            exchangeId,
            new BigDecimal("0.500000000000000000"),
            null,
            null,
            new BigDecimal("100000.000000000000000000"),
            OffsetDateTime.parse("2026-02-13T10:00:00Z")
        );

        TransactionResponse response = transactionService.buy(userId, request);

        assertEquals(0, response.netAmount().compareTo(new BigDecimal("0.5")));
        assertEquals(0, response.totalSpentUsd().compareTo(new BigDecimal("50000")));
        assertEquals(0, response.feeAmount().compareTo(BigDecimal.ZERO));
    }

    @Test
    void buyWithAssetDenominatedFeeReducesNetAmount() {
        BuyTransactionRequest request = new BuyTransactionRequest(
            assetId,
            exchangeId,
            new BigDecimal("1.500000000000000000"),
            new BigDecimal("0.010000000000000000"),
            "btc",
            new BigDecimal("50000.000000000000000000"),
            OffsetDateTime.parse("2026-02-13T10:00:00Z")
        );

        TransactionResponse response = transactionService.buy(userId, request);

        assertEquals(0, response.netAmount().compareTo(new BigDecimal("1.49")));
        assertEquals(0, response.totalSpentUsd().compareTo(new BigDecimal("75000")));
        assertEquals("BTC", response.feeCurrency());
    }

    @Test
    void buyWithUsdFeeIncreasesTotalSpent() {
        BuyTransactionRequest request = new BuyTransactionRequest(
            assetId,
            exchangeId,
            new BigDecimal("1.500000000000000000"),
            new BigDecimal("10.000000000000000000"),
            "USD",
            new BigDecimal("50000.000000000000000000"),
            OffsetDateTime.parse("2026-02-13T10:00:00Z")
        );

        TransactionResponse response = transactionService.buy(userId, request);

        assertEquals(0, response.netAmount().compareTo(new BigDecimal("1.5")));
        assertEquals(0, response.totalSpentUsd().compareTo(new BigDecimal("75010")));
        assertEquals("USD", response.feeCurrency());
    }

    @Test
    void buyWithAssetFeeEqualToGrossIsRejected() {
        BuyTransactionRequest request = new BuyTransactionRequest(
            assetId,
            exchangeId,
            new BigDecimal("1.000000000000000000"),
            new BigDecimal("1.000000000000000000"),
            "BTC",
            new BigDecimal("50000.000000000000000000"),
            OffsetDateTime.parse("2026-02-13T10:00:00Z")
        );

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> transactionService.buy(userId, request)
        );

        assertEquals("netAmount must be positive after asset-denominated fee", ex.getMessage());
    }

    @Test
    void sellWithNoFeeComputesRealizedPnl() {
        when(transactionRepository.findAllByUser_IdAndAsset_IdOrderByTransactionDateDesc(userId, assetId))
            .thenReturn(List.of(
                existingBuy("1.0", "50000", "2026-02-12T10:00:00Z")
            ));

        SellTransactionRequest request = new SellTransactionRequest(
            assetId,
            exchangeId,
            new BigDecimal("0.4"),
            null,
            null,
            new BigDecimal("60000"),
            OffsetDateTime.parse("2026-02-13T10:00:00Z")
        );

        TransactionResponse response = transactionService.sell(userId, request);

        assertEquals(0, response.realizedPnl().compareTo(new BigDecimal("4000")));
        assertEquals(0, response.totalSpentUsd().compareTo(new BigDecimal("24000")));
        assertEquals(TransactionType.SELL, response.transactionType());
    }

    @Test
    void sellWithUsdFeeReducesRealizedPnl() {
        when(transactionRepository.findAllByUser_IdAndAsset_IdOrderByTransactionDateDesc(userId, assetId))
            .thenReturn(List.of(
                existingBuy("1.0", "50000", "2026-02-12T10:00:00Z")
            ));

        SellTransactionRequest request = new SellTransactionRequest(
            assetId,
            exchangeId,
            new BigDecimal("0.4"),
            new BigDecimal("100"),
            "USD",
            new BigDecimal("60000"),
            OffsetDateTime.parse("2026-02-13T10:00:00Z")
        );

        TransactionResponse response = transactionService.sell(userId, request);

        assertEquals(0, response.totalSpentUsd().compareTo(new BigDecimal("23900")));
        assertEquals(0, response.realizedPnl().compareTo(new BigDecimal("3900")));
    }

    @Test
    void sellWithInsufficientBalanceIsRejected() {
        when(transactionRepository.findAllByUser_IdAndAsset_IdOrderByTransactionDateDesc(userId, assetId))
            .thenReturn(List.of(
                existingBuy("0.1", "5000", "2026-02-12T10:00:00Z")
            ));

        SellTransactionRequest request = new SellTransactionRequest(
            assetId,
            exchangeId,
            new BigDecimal("0.2"),
            null,
            null,
            new BigDecimal("60000"),
            OffsetDateTime.parse("2026-02-13T10:00:00Z")
        );

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> transactionService.sell(userId, request)
        );

        assertEquals("Insufficient asset balance for sell transaction", ex.getMessage());
    }

    private static Transaction existingBuy(String netAmount, String totalSpentUsd, String txDate) {
        Transaction tx = new Transaction();
        tx.setTransactionType(TransactionType.BUY);
        tx.setNetAmount(new BigDecimal(netAmount));
        tx.setGrossAmount(new BigDecimal(netAmount));
        tx.setTotalSpentUsd(new BigDecimal(totalSpentUsd));
        tx.setTransactionDate(OffsetDateTime.parse(txDate));
        return tx;
    }
}
