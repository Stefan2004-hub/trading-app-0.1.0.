package com.trading.service.transaction;

import com.trading.domain.entity.Asset;
import com.trading.domain.entity.Exchange;
import com.trading.domain.entity.Transaction;
import com.trading.domain.entity.User;
import com.trading.domain.repository.AssetRepository;
import com.trading.domain.repository.ExchangeRepository;
import com.trading.domain.repository.TransactionRepository;
import com.trading.domain.repository.UserRepository;
import com.trading.dto.transaction.BuyTransactionRequest;
import com.trading.dto.transaction.TransactionResponse;
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
}
