package com.trading.service.transaction;

import com.trading.domain.entity.Asset;
import com.trading.domain.entity.Exchange;
import com.trading.domain.entity.Transaction;
import com.trading.domain.entity.User;
import com.trading.domain.enums.BuyInputMode;
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

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        assetId = UUID.randomUUID();
        exchangeId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);

        Asset asset = new Asset();
        asset.setId(assetId);
        asset.setSymbol("BTC");

        Exchange exchange = new Exchange();
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
        TransactionResponse response = transactionService.buy(
            userId,
            buyRequest(new BigDecimal("0.5"), null, null, null, BuyInputMode.COIN_AMOUNT)
        );

        assertEquals(0, response.netAmount().compareTo(new BigDecimal("0.5")));
        assertEquals(0, response.totalSpentUsd().compareTo(new BigDecimal("50000")));
        assertEquals(0, response.feeAmount().compareTo(BigDecimal.ZERO));
        assertEquals(null, response.feePercentage());
    }

    @Test
    void buyWithUsdInputModeComputesGrossAmount() {
        BuyTransactionRequest request = new BuyTransactionRequest(
            assetId,
            exchangeId,
            null,
            null,
            null,
            null,
            BuyInputMode.USD_AMOUNT,
            new BigDecimal("2500"),
            new BigDecimal("50000"),
            OffsetDateTime.parse("2026-02-13T10:00:00Z")
        );

        TransactionResponse response = transactionService.buy(userId, request);

        assertEquals(0, response.grossAmount().compareTo(new BigDecimal("0.05")));
        assertEquals(0, response.totalSpentUsd().compareTo(new BigDecimal("2500")));
    }

    @Test
    void buyWithFeePercentageCalculatesFeeAmountInCoins() {
        TransactionResponse response = transactionService.buy(
            userId,
            buyRequest(new BigDecimal("1.5"), null, new BigDecimal("0.01"), null, BuyInputMode.COIN_AMOUNT)
        );

        assertEquals(0, response.feeAmount().compareTo(new BigDecimal("0.015")));
        assertEquals(0, response.feePercentage().compareTo(new BigDecimal("0.01")));
        assertEquals("BTC", response.feeCurrency());
        assertEquals(0, response.netAmount().compareTo(new BigDecimal("1.485")));
    }

    @Test
    void buyWithAssetFeeAmountCalculatesFeePercentage() {
        TransactionResponse response = transactionService.buy(
            userId,
            buyRequest(new BigDecimal("2.0"), new BigDecimal("0.02"), null, "BTC", BuyInputMode.COIN_AMOUNT)
        );

        assertEquals(0, response.feePercentage().compareTo(new BigDecimal("0.010000")));
        assertEquals(0, response.netAmount().compareTo(new BigDecimal("1.98")));
    }

    @Test
    void buyWithBothFeeFieldsPrefersFeePercentageForCalculation() {
        TransactionResponse response = transactionService.buy(
            userId,
            buyRequest(new BigDecimal("1.0"), new BigDecimal("0.5"), new BigDecimal("0.01"), "BTC", BuyInputMode.COIN_AMOUNT)
        );

        assertEquals(0, response.feeAmount().compareTo(new BigDecimal("0.01")));
        assertEquals(0, response.netAmount().compareTo(new BigDecimal("0.99")));
    }

    @Test
    void buyWithUnsupportedFeeCurrencyIsRejected() {
        BuyTransactionRequest request = buyRequest(
            new BigDecimal("1.0"),
            new BigDecimal("5.0"),
            null,
            "EUR",
            BuyInputMode.COIN_AMOUNT
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> transactionService.buy(userId, request));
        assertEquals("Unsupported fee currency: EUR", ex.getMessage());
    }

    @Test
    void sellWithNoFeeComputesRealizedPnl() {
        when(transactionRepository.findAllByUser_IdAndAsset_IdOrderByTransactionDateDesc(userId, assetId))
            .thenReturn(List.of(existingBuy("1.0", "50000", "2026-02-12T10:00:00Z")));

        SellTransactionRequest request = sellRequest(new BigDecimal("0.4"), null, null, null);
        TransactionResponse response = transactionService.sell(userId, request);

        assertEquals(0, response.realizedPnl().compareTo(new BigDecimal("4000")));
        assertEquals(0, response.totalSpentUsd().compareTo(new BigDecimal("24000")));
        assertEquals(TransactionType.SELL, response.transactionType());
    }

    @Test
    void sellWithUsdFeeReducesRealizedPnl() {
        when(transactionRepository.findAllByUser_IdAndAsset_IdOrderByTransactionDateDesc(userId, assetId))
            .thenReturn(List.of(existingBuy("1.0", "50000", "2026-02-12T10:00:00Z")));

        SellTransactionRequest request = sellRequest(new BigDecimal("0.4"), new BigDecimal("100"), null, "USD");
        TransactionResponse response = transactionService.sell(userId, request);

        assertEquals(0, response.totalSpentUsd().compareTo(new BigDecimal("23900")));
        assertEquals(0, response.realizedPnl().compareTo(new BigDecimal("3900")));
    }

    @Test
    void sellWithFeePercentageCalculatesCoinFeeAndNetAmount() {
        when(transactionRepository.findAllByUser_IdAndAsset_IdOrderByTransactionDateDesc(userId, assetId))
            .thenReturn(List.of(existingBuy("1.0", "50000", "2026-02-12T10:00:00Z")));

        SellTransactionRequest request = sellRequest(new BigDecimal("0.4"), null, new BigDecimal("0.01"), null);
        TransactionResponse response = transactionService.sell(userId, request);

        assertEquals(0, response.feeAmount().compareTo(new BigDecimal("0.004")));
        assertEquals(0, response.netAmount().compareTo(new BigDecimal("0.396")));
        assertEquals("BTC", response.feeCurrency());
    }

    @Test
    void sellWithUnsupportedFeeCurrencyIsRejected() {
        when(transactionRepository.findAllByUser_IdAndAsset_IdOrderByTransactionDateDesc(userId, assetId))
            .thenReturn(List.of(existingBuy("1.0", "50000", "2026-02-12T10:00:00Z")));

        SellTransactionRequest request = sellRequest(new BigDecimal("0.4"), new BigDecimal("5"), null, "EUR");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> transactionService.sell(userId, request));
        assertEquals("Unsupported fee currency: EUR", ex.getMessage());
    }

    @Test
    void sellWithUsdFeeGreaterThanProceedsIsRejected() {
        when(transactionRepository.findAllByUser_IdAndAsset_IdOrderByTransactionDateDesc(userId, assetId))
            .thenReturn(List.of(existingBuy("1.0", "50000", "2026-02-12T10:00:00Z")));

        SellTransactionRequest request = sellRequest(new BigDecimal("0.1"), new BigDecimal("7000"), null, "USD");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> transactionService.sell(userId, request));
        assertEquals("total sell proceeds cannot be negative", ex.getMessage());
    }

    @Test
    void sellWithInvalidHistoryWithoutPriorBalanceIsRejected() {
        when(transactionRepository.findAllByUser_IdAndAsset_IdOrderByTransactionDateDesc(userId, assetId))
            .thenReturn(List.of(existingSell("0.2", "12000", "2026-02-12T10:00:00Z")));

        SellTransactionRequest request = sellRequest(new BigDecimal("0.1"), null, null, null);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> transactionService.sell(userId, request));
        assertEquals("Invalid position history: sell transaction without balance", ex.getMessage());
    }

    @Test
    void sellWithInsufficientBalanceIsRejected() {
        when(transactionRepository.findAllByUser_IdAndAsset_IdOrderByTransactionDateDesc(userId, assetId))
            .thenReturn(List.of(existingBuy("0.1", "5000", "2026-02-12T10:00:00Z")));

        SellTransactionRequest request = sellRequest(new BigDecimal("0.2"), null, null, null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> transactionService.sell(userId, request));
        assertEquals("Insufficient asset balance for sell transaction", ex.getMessage());
    }

    private BuyTransactionRequest buyRequest(
        BigDecimal grossAmount,
        BigDecimal feeAmount,
        BigDecimal feePercentage,
        String feeCurrency,
        BuyInputMode inputMode
    ) {
        return new BuyTransactionRequest(
            assetId,
            exchangeId,
            grossAmount,
            feeAmount,
            feePercentage,
            feeCurrency,
            inputMode,
            null,
            new BigDecimal("100000"),
            OffsetDateTime.parse("2026-02-13T10:00:00Z")
        );
    }

    private SellTransactionRequest sellRequest(
        BigDecimal grossAmount,
        BigDecimal feeAmount,
        BigDecimal feePercentage,
        String feeCurrency
    ) {
        return new SellTransactionRequest(
            assetId,
            exchangeId,
            grossAmount,
            feeAmount,
            feePercentage,
            feeCurrency,
            new BigDecimal("60000"),
            OffsetDateTime.parse("2026-02-13T10:00:00Z")
        );
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

    private static Transaction existingSell(String grossAmount, String proceedsUsd, String txDate) {
        Transaction tx = new Transaction();
        tx.setTransactionType(TransactionType.SELL);
        tx.setGrossAmount(new BigDecimal(grossAmount));
        tx.setTotalSpentUsd(new BigDecimal(proceedsUsd));
        tx.setTransactionDate(OffsetDateTime.parse(txDate));
        return tx;
    }
}
