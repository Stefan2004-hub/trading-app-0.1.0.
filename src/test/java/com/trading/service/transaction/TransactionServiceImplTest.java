package com.trading.service.transaction;

import com.trading.domain.entity.Asset;
import com.trading.domain.entity.AccumulationTrade;
import com.trading.domain.entity.Exchange;
import com.trading.domain.entity.PricePeak;
import com.trading.domain.entity.Transaction;
import com.trading.domain.entity.User;
import com.trading.domain.enums.BuyInputMode;
import com.trading.domain.enums.TransactionListView;
import com.trading.domain.enums.TransactionType;
import com.trading.domain.repository.AssetRepository;
import com.trading.domain.repository.AccumulationTradeRepository;
import com.trading.domain.repository.ExchangeRepository;
import com.trading.domain.repository.PricePeakRepository;
import com.trading.domain.repository.TransactionRepository;
import com.trading.domain.repository.UserRepository;
import com.trading.dto.transaction.BuyTransactionRequest;
import com.trading.dto.transaction.SellTransactionRequest;
import com.trading.dto.transaction.TransactionResponse;
import com.trading.dto.transaction.UpdateTransactionNetAmountRequest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AccumulationTradeRepository accumulationTradeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AssetRepository assetRepository;
    @Mock
    private ExchangeRepository exchangeRepository;
    @Mock
    private PricePeakRepository pricePeakRepository;

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

        lenient().when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        lenient().when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        lenient().when(exchangeRepository.findById(exchangeId)).thenReturn(Optional.of(exchange));
        lenient().when(accumulationTradeRepository.findAllByUser_IdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        lenient().when(accumulationTradeRepository.findAllByUser_IdAndReentryTransaction_Id(eq(userId), any(UUID.class)))
            .thenReturn(List.of());
        lenient().when(accumulationTradeRepository.findAllByUser_IdAndExitTransaction_Id(eq(userId), any(UUID.class)))
            .thenReturn(List.of());
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
    void buyWithUnsupportedFeeCurrencyPersistsFeeWithoutMutatingNetAmount() {
        BuyTransactionRequest request = buyRequest(
            new BigDecimal("1.0"),
            new BigDecimal("5.0"),
            null,
            "EUR",
            BuyInputMode.COIN_AMOUNT
        );

        TransactionResponse response = transactionService.buy(userId, request);

        assertEquals(0, response.netAmount().compareTo(new BigDecimal("1.0")));
        assertEquals(0, response.totalSpentUsd().compareTo(new BigDecimal("100000")));
        assertEquals("EUR", response.feeCurrency());
    }

    @Test
    void sellWithNoFeeComputesRealizedPnl() {
        when(transactionRepository.findAllByUser_IdAndAsset_IdOrderByTransactionDateDesc(userId, assetId))
            .thenReturn(List.of(existingBuy("1.0", "50000", "2026-02-12T10:00:00Z")));

        SellTransactionRequest request = sellRequest(new BigDecimal("0.4"), null, null, null);
        TransactionResponse response = transactionService.sell(userId, request);

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
    }

    @Test
    void sellWithFeePercentageCalculatesUsdFeeAndReducesUsdProceeds() {
        when(transactionRepository.findAllByUser_IdAndAsset_IdOrderByTransactionDateDesc(userId, assetId))
            .thenReturn(List.of(existingBuy("1.0", "50000", "2026-02-12T10:00:00Z")));

        SellTransactionRequest request = sellRequest(new BigDecimal("0.4"), null, new BigDecimal("0.01"), null);
        TransactionResponse response = transactionService.sell(userId, request);

        assertEquals(0, response.feeAmount().compareTo(new BigDecimal("240.000")));
        assertEquals(0, response.netAmount().compareTo(new BigDecimal("0.4")));
        assertEquals(0, response.totalSpentUsd().compareTo(new BigDecimal("23760.000")));
        assertEquals("USD", response.feeCurrency());
    }

    @Test
    void sellWithUnsupportedFeeCurrencyPersistsFeeWithoutMutatingNetAmount() {
        when(transactionRepository.findAllByUser_IdAndAsset_IdOrderByTransactionDateDesc(userId, assetId))
            .thenReturn(List.of(existingBuy("1.0", "50000", "2026-02-12T10:00:00Z")));

        SellTransactionRequest request = sellRequest(new BigDecimal("0.4"), new BigDecimal("5"), null, "EUR");

        TransactionResponse response = transactionService.sell(userId, request);
        assertEquals(0, response.netAmount().compareTo(new BigDecimal("0.4")));
        assertEquals(0, response.totalSpentUsd().compareTo(new BigDecimal("24000")));
        assertEquals("EUR", response.feeCurrency());
    }

    @Test
    void sellWithUsdFeeGreaterThanProceedsAllowsNegativeProceeds() {
        when(transactionRepository.findAllByUser_IdAndAsset_IdOrderByTransactionDateDesc(userId, assetId))
            .thenReturn(List.of(existingBuy("1.0", "50000", "2026-02-12T10:00:00Z")));

        SellTransactionRequest request = sellRequest(new BigDecimal("0.1"), new BigDecimal("7000"), null, "USD");

        TransactionResponse response = transactionService.sell(userId, request);
        assertEquals(0, response.totalSpentUsd().compareTo(new BigDecimal("-1000")));
    }

    @Test
    void sellWithInvalidHistoryWithoutPriorBalanceStillCalculatesUsingZeroCostBasis() {
        when(transactionRepository.findAllByUser_IdAndAsset_IdOrderByTransactionDateDesc(userId, assetId))
            .thenReturn(List.of(existingSell("0.2", "12000", "2026-02-12T10:00:00Z")));

        SellTransactionRequest request = sellRequest(new BigDecimal("0.1"), null, null, null);

        TransactionResponse response = transactionService.sell(userId, request);
        assertEquals(0, response.totalSpentUsd().compareTo(new BigDecimal("6000")));
    }

    @Test
    void sellWithInsufficientBalanceStillProcessesTransaction() {
        when(transactionRepository.findAllByUser_IdAndAsset_IdOrderByTransactionDateDesc(userId, assetId))
            .thenReturn(List.of(existingBuy("0.1", "5000", "2026-02-12T10:00:00Z")));

        SellTransactionRequest request = sellRequest(new BigDecimal("0.2"), null, null, null);

        TransactionResponse response = transactionService.sell(userId, request);
        assertEquals(0, response.grossAmount().compareTo(new BigDecimal("0.2")));
        assertEquals(TransactionType.SELL, response.transactionType());
    }

    @Test
    void fullSellConsumesExactCostBasisWithoutResidualInvestedOrBalance() {
        List<Transaction> history = inMemoryHistory(
            existingBuy("1", "0.4", "2026-02-10T10:00:00Z"),
            existingBuy("2", "0.6", "2026-02-11T10:00:00Z")
        );

        SellTransactionRequest request = sellRequest(new BigDecimal("3"), null, null, null);
        TransactionResponse response = transactionService.sell(userId, request);

        assertEquals(0, response.realizedPnl().compareTo(new BigDecimal("179999")));
        assertEquals(0, calculateRemainingInvestedUsd(history).compareTo(BigDecimal.ZERO));
        assertEquals(0, calculateCurrentBalance(history).compareTo(BigDecimal.ZERO));
    }

    @Test
    void finalSellAfterPartialSellLeavesNoResidualInvestedOrBalance() {
        List<Transaction> history = inMemoryHistory(
            existingBuy("1", "0.4", "2026-02-10T10:00:00Z"),
            existingBuy("2", "0.6", "2026-02-11T10:00:00Z")
        );

        SellTransactionRequest firstSellRequest = new SellTransactionRequest(
            assetId,
            exchangeId,
            new BigDecimal("1"),
            null,
            null,
            null,
            new BigDecimal("60000"),
            OffsetDateTime.parse("2026-02-12T10:00:00Z")
        );
        SellTransactionRequest finalSellRequest = new SellTransactionRequest(
            assetId,
            exchangeId,
            new BigDecimal("2"),
            null,
            null,
            null,
            new BigDecimal("60000"),
            OffsetDateTime.parse("2026-02-13T10:00:00Z")
        );

        TransactionResponse firstSell = transactionService.sell(userId, firstSellRequest);
        TransactionResponse finalSell = transactionService.sell(userId, finalSellRequest);

        assertEquals(0, firstSell.realizedPnl().compareTo(new BigDecimal("59999.666666666666666667")));
        assertEquals(0, finalSell.realizedPnl().compareTo(new BigDecimal("119999.333333333333333333")));
        assertEquals(0, calculateRemainingInvestedUsd(history).compareTo(BigDecimal.ZERO));
        assertEquals(0, calculateCurrentBalance(history).compareTo(BigDecimal.ZERO));
    }

    @Test
    void deleteBuyTransactionClearsReferencedPricePeakWhenNoReplacementBuyExists() {
        Transaction buyTx = new Transaction();
        buyTx.setId(UUID.randomUUID());
        buyTx.setTransactionType(TransactionType.BUY);
        Asset asset = new Asset();
        asset.setId(assetId);
        buyTx.setAsset(asset);

        PricePeak pricePeak = new PricePeak();
        pricePeak.setAsset(asset);
        pricePeak.setLastBuyTransaction(buyTx);
        pricePeak.setActive(true);

        when(transactionRepository.findByIdAndUser_Id(buyTx.getId(), userId)).thenReturn(Optional.of(buyTx));
        when(pricePeakRepository.findByUser_IdAndAsset_Id(userId, assetId)).thenReturn(Optional.of(pricePeak));
        when(transactionRepository.findAllByUser_IdAndAsset_IdAndTransactionTypeOrderByTransactionDateDesc(
            userId, assetId, TransactionType.BUY
        )).thenReturn(List.of(buyTx));
        when(transactionRepository.findAllByUser_IdAndAsset_IdOrderByTransactionDateDesc(userId, assetId))
            .thenReturn(List.of());
        AccumulationTrade linkedTrade = new AccumulationTrade();
        linkedTrade.setId(UUID.randomUUID());
        when(accumulationTradeRepository.findAllByUser_IdAndReentryTransaction_Id(userId, buyTx.getId()))
            .thenReturn(List.of(linkedTrade));
        when(accumulationTradeRepository.findAllByUser_IdAndExitTransaction_Id(userId, buyTx.getId()))
            .thenReturn(List.of());

        transactionService.deleteTransaction(userId, buyTx.getId());

        assertEquals(null, pricePeak.getLastBuyTransaction());
        assertEquals(false, pricePeak.getActive());
        verify(accumulationTradeRepository).deleteAll(List.of(linkedTrade));
        verify(pricePeakRepository).save(pricePeak);
    }

    @Test
    void deleteSellTransactionDoesNotTouchPricePeak() {
        Transaction sellTx = new Transaction();
        sellTx.setId(UUID.randomUUID());
        sellTx.setTransactionType(TransactionType.SELL);
        Asset asset = new Asset();
        asset.setId(assetId);
        sellTx.setAsset(asset);

        when(transactionRepository.findByIdAndUser_Id(sellTx.getId(), userId)).thenReturn(Optional.of(sellTx));
        when(transactionRepository.findAllByUser_IdAndAsset_IdOrderByTransactionDateDesc(userId, assetId))
            .thenReturn(List.of());
        AccumulationTrade linkedTrade = new AccumulationTrade();
        linkedTrade.setId(UUID.randomUUID());
        when(accumulationTradeRepository.findAllByUser_IdAndReentryTransaction_Id(userId, sellTx.getId()))
            .thenReturn(List.of());
        when(accumulationTradeRepository.findAllByUser_IdAndExitTransaction_Id(userId, sellTx.getId()))
            .thenReturn(List.of(linkedTrade));

        transactionService.deleteTransaction(userId, sellTx.getId());

        verify(accumulationTradeRepository).deleteAll(List.of(linkedTrade));
        verify(pricePeakRepository, never()).save(any(PricePeak.class));
    }

    @Test
    void updateTransactionNetAmountUpdatesOnlyNetAmountField() {
        UUID transactionId = UUID.randomUUID();
        Transaction tx = new Transaction();
        tx.setId(transactionId);
        tx.setTransactionType(TransactionType.BUY);
        tx.setGrossAmount(new BigDecimal("1.0"));
        tx.setNetAmount(new BigDecimal("1.0"));
        tx.setUnitPriceUsd(new BigDecimal("50000"));
        tx.setTotalSpentUsd(new BigDecimal("50000"));
        tx.setTransactionDate(OffsetDateTime.parse("2026-02-13T10:00:00Z"));
        Asset asset = new Asset();
        asset.setId(assetId);
        asset.setSymbol("BTC");
        tx.setAsset(asset);
        Exchange exchange = new Exchange();
        exchange.setId(exchangeId);
        tx.setExchange(exchange);
        User user = new User();
        user.setId(userId);
        tx.setUser(user);

        when(transactionRepository.findByIdAndUser_Id(transactionId, userId)).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0, Transaction.class));

        TransactionResponse response = transactionService.updateTransactionNetAmount(
            userId,
            transactionId,
            new UpdateTransactionNetAmountRequest(new BigDecimal("0.75"))
        );

        assertEquals(0, response.netAmount().compareTo(new BigDecimal("0.75")));
        assertEquals(0, tx.getGrossAmount().compareTo(new BigDecimal("1.0")));
        assertEquals(0, tx.getTotalSpentUsd().compareTo(new BigDecimal("50000")));
        verify(transactionRepository, never()).findAllByUser_IdAndAsset_IdOrderByTransactionDateDesc(userId, assetId);
    }

    @Test
    void listMarksBuyAsMatchedEvenWhenSellIsOutsideCurrentPage() {
        UUID buyId = UUID.randomUUID();
        UUID sellId = UUID.randomUUID();

        Transaction buy = existingBuy("1.0", "50000", "2026-02-12T10:00:00Z");
        buy.setId(buyId);
        buy.setUser(user(userId));
        buy.setAsset(asset(assetId, "BTC"));
        buy.setExchange(exchange(exchangeId));

        Transaction sell = existingSell("1.0", "60000", "2026-02-13T10:00:00Z");
        sell.setId(sellId);
        sell.setUser(user(userId));
        sell.setAsset(asset(assetId, "BTC"));
        sell.setExchange(exchange(exchangeId));

        PageRequest pageRequest = PageRequest.of(0, 1);
        when(transactionRepository.findByUser_IdAndSearch(eq(userId), eq(null), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(List.of(buy), pageRequest, 1));
        when(transactionRepository.findAllByUser_IdOrderByTransactionDateDesc(userId))
            .thenReturn(List.of(sell, buy));

        TransactionResponse response = transactionService.list(userId, 0, 1, null, TransactionListView.OPEN, 1).getContent().get(0);

        assertEquals(true, response.matched());
        assertEquals(sellId, response.matchedTransactionId());
    }

    @Test
    void listMatchesWhenBuyAndSellAmountsDifferWithinEpsilon() {
        UUID buyId = UUID.randomUUID();
        UUID sellId = UUID.randomUUID();

        Transaction buy = existingBuy("1.0000000000005", "50000", "2026-02-12T10:00:00Z");
        buy.setId(buyId);
        buy.setUser(user(userId));
        buy.setAsset(asset(assetId, "BTC"));
        buy.setExchange(exchange(exchangeId));

        Transaction sell = existingSell("1.0000000000000", "60000", "2026-02-13T10:00:00Z");
        sell.setId(sellId);
        sell.setUser(user(userId));
        sell.setAsset(asset(assetId, "BTC"));
        sell.setExchange(exchange(exchangeId));

        PageRequest pageRequest = PageRequest.of(0, 1);
        when(transactionRepository.findByUser_IdAndSearch(eq(userId), eq(null), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(List.of(sell), pageRequest, 1));
        when(transactionRepository.findAllByUser_IdOrderByTransactionDateDesc(userId))
            .thenReturn(List.of(sell, buy));

        TransactionResponse response = transactionService.list(userId, 0, 1, null, TransactionListView.OPEN, 1).getContent().get(0);

        assertEquals(true, response.matched());
        assertEquals(buyId, response.matchedTransactionId());
    }

    @Test
    void listDoesNotMatchWhenBuyAndSellAmountsDifferAboveEpsilon() {
        UUID sellId = UUID.randomUUID();

        Transaction buy = existingBuy("1.000000000002", "50000", "2026-02-12T10:00:00Z");
        buy.setId(UUID.randomUUID());
        buy.setUser(user(userId));
        buy.setAsset(asset(assetId, "BTC"));
        buy.setExchange(exchange(exchangeId));

        Transaction sell = existingSell("1.000000000000", "60000", "2026-02-13T10:00:00Z");
        sell.setId(sellId);
        sell.setUser(user(userId));
        sell.setAsset(asset(assetId, "BTC"));
        sell.setExchange(exchange(exchangeId));

        PageRequest pageRequest = PageRequest.of(0, 1);
        when(transactionRepository.findByUser_IdAndSearch(eq(userId), eq(null), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(List.of(sell), pageRequest, 1));
        when(transactionRepository.findAllByUser_IdOrderByTransactionDateDesc(userId))
            .thenReturn(List.of(sell, buy));

        TransactionResponse response = transactionService.list(userId, 0, 1, null, TransactionListView.OPEN, 1).getContent().get(0);

        assertEquals(false, response.matched());
        assertNull(response.matchedTransactionId());
    }

    @Test
    void listMatchesSellToClosestBuyWithinEpsilon() {
        UUID buyFartherId = UUID.randomUUID();
        UUID buyCloserId = UUID.randomUUID();
        UUID sellId = UUID.randomUUID();

        Transaction buyFarther = existingBuy("1.0000000000009", "50000", "2026-02-12T10:00:00Z");
        buyFarther.setId(buyFartherId);
        buyFarther.setUser(user(userId));
        buyFarther.setAsset(asset(assetId, "BTC"));
        buyFarther.setExchange(exchange(exchangeId));

        Transaction buyCloser = existingBuy("1.0000000000001", "50000", "2026-02-12T11:00:00Z");
        buyCloser.setId(buyCloserId);
        buyCloser.setUser(user(userId));
        buyCloser.setAsset(asset(assetId, "BTC"));
        buyCloser.setExchange(exchange(exchangeId));

        Transaction sell = existingSell("1.0000000000000", "60000", "2026-02-13T10:00:00Z");
        sell.setId(sellId);
        sell.setUser(user(userId));
        sell.setAsset(asset(assetId, "BTC"));
        sell.setExchange(exchange(exchangeId));

        PageRequest pageRequest = PageRequest.of(0, 1);
        when(transactionRepository.findByUser_IdAndSearch(eq(userId), eq(null), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(List.of(sell), pageRequest, 1));
        when(transactionRepository.findAllByUser_IdOrderByTransactionDateDesc(userId))
            .thenReturn(List.of(sell, buyCloser, buyFarther));

        TransactionResponse response = transactionService.list(userId, 0, 1, null, TransactionListView.OPEN, 1).getContent().get(0);

        assertEquals(true, response.matched());
        assertEquals(buyCloserId, response.matchedTransactionId());
    }

    @Test
    void listMatchesSellToOldestBuyWhenDifferencesAreEqual() {
        UUID buyOlderId = UUID.randomUUID();
        UUID buyNewerId = UUID.randomUUID();
        UUID sellId = UUID.randomUUID();

        Transaction buyOlder = existingBuy("1.0000000000005", "50000", "2026-02-12T10:00:00Z");
        buyOlder.setId(buyOlderId);
        buyOlder.setUser(user(userId));
        buyOlder.setAsset(asset(assetId, "BTC"));
        buyOlder.setExchange(exchange(exchangeId));

        Transaction buyNewer = existingBuy("0.9999999999995", "50000", "2026-02-12T11:00:00Z");
        buyNewer.setId(buyNewerId);
        buyNewer.setUser(user(userId));
        buyNewer.setAsset(asset(assetId, "BTC"));
        buyNewer.setExchange(exchange(exchangeId));

        Transaction sell = existingSell("1.0000000000000", "60000", "2026-02-13T10:00:00Z");
        sell.setId(sellId);
        sell.setUser(user(userId));
        sell.setAsset(asset(assetId, "BTC"));
        sell.setExchange(exchange(exchangeId));

        PageRequest pageRequest = PageRequest.of(0, 1);
        when(transactionRepository.findByUser_IdAndSearch(eq(userId), eq(null), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(List.of(sell), pageRequest, 1));
        when(transactionRepository.findAllByUser_IdOrderByTransactionDateDesc(userId))
            .thenReturn(List.of(sell, buyNewer, buyOlder));

        TransactionResponse response = transactionService.list(userId, 0, 1, null, TransactionListView.OPEN, 1).getContent().get(0);

        assertEquals(true, response.matched());
        assertEquals(buyOlderId, response.matchedTransactionId());
    }

    @Test
    void listDoesNotMatchAcrossDifferentExchanges() {
        UUID sellId = UUID.randomUUID();
        UUID otherExchangeId = UUID.randomUUID();

        Transaction buy = existingBuy("1.0000000000000", "50000", "2026-02-12T10:00:00Z");
        buy.setId(UUID.randomUUID());
        buy.setUser(user(userId));
        buy.setAsset(asset(assetId, "BTC"));
        buy.setExchange(exchange(exchangeId));

        Transaction sell = existingSell("1.0000000000005", "60000", "2026-02-13T10:00:00Z");
        sell.setId(sellId);
        sell.setUser(user(userId));
        sell.setAsset(asset(assetId, "BTC"));
        sell.setExchange(exchange(otherExchangeId));

        PageRequest pageRequest = PageRequest.of(0, 1);
        when(transactionRepository.findByUser_IdAndSearch(eq(userId), eq(null), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(List.of(sell), pageRequest, 1));
        when(transactionRepository.findAllByUser_IdOrderByTransactionDateDesc(userId))
            .thenReturn(List.of(sell, buy));

        TransactionResponse response = transactionService.list(userId, 0, 1, null, TransactionListView.OPEN, 1).getContent().get(0);

        assertEquals(false, response.matched());
        assertNull(response.matchedTransactionId());
    }

    @Test
    void matchedViewPaginatesByPairGroupsWithoutSplittingPairs() {
        UUID buyAId = UUID.randomUUID();
        UUID sellAId = UUID.randomUUID();
        UUID buyBId = UUID.randomUUID();
        UUID sellBId = UUID.randomUUID();

        Transaction buyA = existingBuy("1.0", "50000", "2026-02-10T10:00:00Z");
        buyA.setId(buyAId);
        buyA.setUser(user(userId));
        buyA.setAsset(asset(assetId, "BTC"));
        buyA.setExchange(exchange(exchangeId));

        Transaction sellA = existingSell("1.0", "60000", "2026-02-11T10:00:00Z");
        sellA.setId(sellAId);
        sellA.setUser(user(userId));
        sellA.setAsset(asset(assetId, "BTC"));
        sellA.setExchange(exchange(exchangeId));

        Transaction buyB = existingBuy("2.0", "50000", "2026-02-12T10:00:00Z");
        buyB.setId(buyBId);
        buyB.setUser(user(userId));
        buyB.setAsset(asset(assetId, "BTC"));
        buyB.setExchange(exchange(exchangeId));

        Transaction sellB = existingSell("2.0", "60000", "2026-02-13T10:00:00Z");
        sellB.setId(sellBId);
        sellB.setUser(user(userId));
        sellB.setAsset(asset(assetId, "BTC"));
        sellB.setExchange(exchange(exchangeId));

        when(transactionRepository.findAllByUser_IdOrderByTransactionDateDesc(userId))
            .thenReturn(List.of(sellB, buyB, sellA, buyA));

        var page0 = transactionService.list(userId, 0, 20, null, TransactionListView.MATCHED, 1);
        var page1 = transactionService.list(userId, 1, 20, null, TransactionListView.MATCHED, 1);

        assertEquals(4, page0.getTotalElements());
        assertEquals(2, page0.getContent().size());
        assertEquals(TransactionType.BUY, page0.getContent().get(0).transactionType());
        assertEquals(TransactionType.SELL, page0.getContent().get(1).transactionType());
        assertEquals(page0.getContent().get(1).id(), page0.getContent().get(0).matchedTransactionId());

        assertEquals(2, page1.getContent().size());
        assertEquals(TransactionType.BUY, page1.getContent().get(0).transactionType());
        assertEquals(TransactionType.SELL, page1.getContent().get(1).transactionType());
        assertEquals(page1.getContent().get(1).id(), page1.getContent().get(0).matchedTransactionId());
    }

    @Test
    void matchedViewSearchMatchesEitherSideOfPair() {
        UUID buyId = UUID.randomUUID();
        UUID sellId = UUID.randomUUID();

        Transaction buy = existingBuy("1.0", "50000", "2026-02-12T10:00:00Z");
        buy.setId(buyId);
        buy.setUser(user(userId));
        buy.setAsset(asset(assetId, "BTC"));
        buy.setExchange(exchange(exchangeId));

        Transaction sell = existingSell("1.0", "60000", "2026-02-13T10:00:00Z");
        sell.setId(sellId);
        sell.setUser(user(userId));
        sell.setAsset(asset(assetId, "BTC"));
        sell.setExchange(exchange(exchangeId));

        when(transactionRepository.findAllByUser_IdOrderByTransactionDateDesc(userId))
            .thenReturn(List.of(sell, buy));

        var page = transactionService.list(userId, 0, 20, "BTC", TransactionListView.MATCHED, 10);

        assertEquals(2, page.getTotalElements());
        assertEquals(2, page.getContent().size());
        assertEquals(true, page.getContent().get(0).matched());
        assertEquals(true, page.getContent().get(1).matched());
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
        Asset asset = new Asset();
        asset.setSymbol("BTC");
        tx.setAsset(asset);
        tx.setNetAmount(new BigDecimal(netAmount));
        tx.setGrossAmount(new BigDecimal(netAmount));
        tx.setFeeAmount(BigDecimal.ZERO);
        tx.setFeeCurrency(null);
        tx.setFeePercentage(null);
        tx.setUnitPriceUsd(new BigDecimal(totalSpentUsd).divide(new BigDecimal(netAmount), 18, java.math.RoundingMode.HALF_UP));
        tx.setTotalSpentUsd(new BigDecimal(totalSpentUsd));
        tx.setTransactionDate(OffsetDateTime.parse(txDate));
        return tx;
    }

    private static Transaction existingSell(String grossAmount, String proceedsUsd, String txDate) {
        Transaction tx = new Transaction();
        tx.setTransactionType(TransactionType.SELL);
        Asset asset = new Asset();
        asset.setSymbol("BTC");
        tx.setAsset(asset);
        tx.setGrossAmount(new BigDecimal(grossAmount));
        tx.setNetAmount(new BigDecimal(grossAmount));
        tx.setFeeAmount(BigDecimal.ZERO);
        tx.setFeeCurrency(null);
        tx.setFeePercentage(null);
        tx.setUnitPriceUsd(new BigDecimal(proceedsUsd).divide(new BigDecimal(grossAmount), 18, java.math.RoundingMode.HALF_UP));
        tx.setTotalSpentUsd(new BigDecimal(proceedsUsd));
        tx.setTransactionDate(OffsetDateTime.parse(txDate));
        return tx;
    }

    private List<Transaction> inMemoryHistory(Transaction... initialTransactions) {
        List<Transaction> history = new ArrayList<>(List.of(initialTransactions));

        when(transactionRepository.findAllByUser_IdAndAsset_IdOrderByTransactionDateDesc(userId, assetId))
            .thenAnswer(invocation -> new ArrayList<>(history));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction tx = invocation.getArgument(0, Transaction.class);
            if (tx.getId() == null) {
                tx.setId(UUID.randomUUID());
            }
            history.add(tx);
            return tx;
        });
        when(transactionRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<Transaction> saved = invocation.getArgument(0);
            history.clear();
            history.addAll(saved);
            return saved;
        });
        when(transactionRepository.findByIdAndUser_Id(any(UUID.class), eq(userId))).thenAnswer(invocation -> {
            UUID id = invocation.getArgument(0, UUID.class);
            return history.stream()
                .filter(tx -> id.equals(tx.getId()))
                .findFirst();
        });

        return history;
    }

    private static BigDecimal calculateRemainingInvestedUsd(List<Transaction> history) {
        return history.stream()
            .map(tx -> tx.getTransactionType() == TransactionType.BUY
                ? tx.getTotalSpentUsd()
                : tx.getTotalSpentUsd().subtract(tx.getRealizedPnl()).negate())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal calculateCurrentBalance(List<Transaction> history) {
        return history.stream()
            .map(tx -> tx.getTransactionType() == TransactionType.BUY
                ? tx.getNetAmount()
                : tx.getNetAmount().negate())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static User user(UUID id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private static Asset asset(UUID id, String symbol) {
        Asset asset = new Asset();
        asset.setId(id);
        asset.setSymbol(symbol);
        return asset;
    }

    private static Exchange exchange(UUID id) {
        Exchange exchange = new Exchange();
        exchange.setId(id);
        return exchange;
    }
}
