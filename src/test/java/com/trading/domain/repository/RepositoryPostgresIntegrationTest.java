package com.trading.domain.repository;

import com.trading.domain.entity.AccumulationTrade;
import com.trading.domain.entity.Asset;
import com.trading.domain.entity.Exchange;
import com.trading.domain.entity.PricePeak;
import com.trading.domain.entity.Transaction;
import com.trading.domain.entity.User;
import com.trading.domain.enums.AccumulationTradeStatus;
import com.trading.domain.enums.AuthProvider;
import com.trading.domain.enums.TransactionType;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RepositoryPostgresIntegrationTest {

    private static EmbeddedPostgres postgres;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccumulationTradeRepository accumulationTradeRepository;

    @Autowired
    private PricePeakRepository pricePeakRepository;

    @AfterAll
    static void stopPostgres() throws IOException {
        if (postgres != null) {
            postgres.close();
        }
    }

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        ensurePostgresStarted();
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("app.historical.enabled", () -> false);
    }

    private static void ensurePostgresStarted() {
        if (postgres == null) {
            try {
                postgres = EmbeddedPostgres.builder().start();
            } catch (IOException exception) {
                throw new RuntimeException("Failed to start embedded Postgres", exception);
            }
        }
    }

    @Test
    void findOpenTransactionsAllowsNullFiltersAndPreservesNestedSort() {
        User user = persistUser("tx-null");
        User otherUser = persistUser("tx-other");
        Asset ada = persistAsset("ADN0", "Cardano Null");
        Asset btc = persistAsset("BTN0", "Bitcoin Null");
        Exchange binance = persistExchange("Binance Null", "BNN0");
        Exchange kraken = persistExchange("Kraken Null", "KRN0");

        Transaction first = persistTransaction(
            user,
            ada,
            binance,
            TransactionType.BUY,
            "10",
            "10",
            "1.20",
            "12.0",
            "2026-01-02T10:00:00Z"
        );
        Transaction second = persistTransaction(
            user,
            btc,
            kraken,
            TransactionType.BUY,
            "1",
            "1",
            "50000",
            "50000",
            "2026-01-03T10:00:00Z"
        );
        persistTransaction(
            otherUser,
            btc,
            kraken,
            TransactionType.BUY,
            "2",
            "2",
            "51000",
            "102000",
            "2026-01-04T10:00:00Z"
        );

        var page = transactionRepository.findOpenTransactions(
            user.getId(),
            null,
            null,
            null,
            PageRequest.of(
                0,
                20,
                Sort.by(
                    Sort.Order.asc("exchange.name"),
                    Sort.Order.asc("asset.symbol"),
                    Sort.Order.desc("transactionDate")
                )
            )
        );

        assertEquals(2, page.getTotalElements());
        assertEquals(List.of(first.getId(), second.getId()), page.getContent().stream().map(Transaction::getId).toList());
    }

    @Test
    void findOpenTransactionsSupportsIndependentDateBoundsAndSearch() {
        User user = persistUser("tx-date");
        Asset btc = persistAsset("BTCD", "Bitcoin Date");
        Exchange coinbase = persistExchange("Coinbase Date", "CBD");

        Transaction beforeWindow = persistTransaction(
            user,
            btc,
            coinbase,
            TransactionType.BUY,
            "1",
            "1",
            "40000",
            "40000",
            "2025-12-31T23:00:00Z"
        );
        Transaction insideWindow = persistTransaction(
            user,
            btc,
            coinbase,
            TransactionType.BUY,
            "1",
            "1",
            "41000",
            "41000",
            "2026-01-01T12:00:00Z"
        );
        Transaction afterWindow = persistTransaction(
            user,
            btc,
            coinbase,
            TransactionType.BUY,
            "1",
            "1",
            "42000",
            "42000",
            "2026-01-03T12:00:00Z"
        );

        var fromOnlyPage = transactionRepository.findOpenTransactions(
            user.getId(),
            null,
            OffsetDateTime.parse("2026-01-01T00:00:00Z"),
            null,
            PageRequest.of(0, 20, Sort.by(Sort.Order.asc("transactionDate")))
        );
        var toOnlyPage = transactionRepository.findOpenTransactions(
            user.getId(),
            null,
            null,
            OffsetDateTime.parse("2026-01-02T00:00:00Z"),
            PageRequest.of(0, 20, Sort.by(Sort.Order.asc("transactionDate")))
        );
        var filteredPage = transactionRepository.findOpenTransactions(
            user.getId(),
            "%bit%",
            OffsetDateTime.parse("2026-01-01T00:00:00Z"),
            OffsetDateTime.parse("2026-01-02T00:00:00Z"),
            PageRequest.of(0, 20, Sort.by(Sort.Order.asc("transactionDate")))
        );

        assertEquals(List.of(insideWindow.getId(), afterWindow.getId()), fromOnlyPage.getContent().stream().map(Transaction::getId).toList());
        assertEquals(List.of(beforeWindow.getId(), insideWindow.getId()), toOnlyPage.getContent().stream().map(Transaction::getId).toList());
        assertEquals(List.of(insideWindow.getId()), filteredPage.getContent().stream().map(Transaction::getId).toList());
    }

    @Test
    void summarizeByAssetAndStatusReturnsGroupedRowsWithoutNullableFilters() {
        User user = persistUser("acc-summary");
        Asset ada = persistAsset("ADAS", "Ada Summary");
        Asset btc = persistAsset("BTCS", "Bitcoin Summary");
        Exchange exchange = persistExchange("Summary Exchange", "SUM");

        Transaction adaExit = persistTransaction(
            user,
            ada,
            exchange,
            TransactionType.SELL,
            "1.0",
            "1.0",
            "2.0",
            "2.0",
            "2026-02-01T10:00:00Z"
        );
        Transaction adaReentry = persistTransaction(
            user,
            ada,
            exchange,
            TransactionType.BUY,
            "1.1",
            "1.1",
            "1.8",
            "1.98",
            "2026-02-02T10:00:00Z"
        );
        Transaction btcExit = persistTransaction(
            user,
            btc,
            exchange,
            TransactionType.SELL,
            "2.0",
            "2.0",
            "5.0",
            "10.0",
            "2026-02-03T10:00:00Z"
        );
        Transaction btcReentry = persistTransaction(
            user,
            btc,
            exchange,
            TransactionType.BUY,
            "2.4",
            "2.4",
            "4.0",
            "9.6",
            "2026-02-04T10:00:00Z"
        );
        persistAccumulationTrade(user, ada, adaExit, adaReentry, AccumulationTradeStatus.CLOSED, "1.0", "1.1");
        persistAccumulationTrade(user, btc, btcExit, btcReentry, AccumulationTradeStatus.CLOSED, "2.0", "2.4");

        List<UUID> assetIds = accumulationTradeRepository.summarizeByAssetAndStatus(
            user.getId(),
            AccumulationTradeStatus.CLOSED
        ).stream().map(row -> row.getAssetId()).toList();

        assertEquals(Set.of(ada.getId(), btc.getId()), new HashSet<>(assetIds));
    }

    @Test
    void summarizeByAssetAndStatusAndAssetIdFiltersToSingleAsset() {
        User user = persistUser("acc-asset");
        Asset ada = persistAsset("ADAF", "Ada Filter");
        Asset btc = persistAsset("BTCF", "Bitcoin Filter");
        Exchange exchange = persistExchange("Filter Exchange", "FIL");

        Transaction adaExit = persistTransaction(
            user,
            ada,
            exchange,
            TransactionType.SELL,
            "1.5",
            "1.5",
            "2.0",
            "3.0",
            "2026-03-01T10:00:00Z"
        );
        Transaction adaReentry = persistTransaction(
            user,
            ada,
            exchange,
            TransactionType.BUY,
            "1.7",
            "1.7",
            "1.7",
            "2.89",
            "2026-03-02T10:00:00Z"
        );
        Transaction btcExit = persistTransaction(
            user,
            btc,
            exchange,
            TransactionType.SELL,
            "3.0",
            "3.0",
            "4.0",
            "12.0",
            "2026-03-03T10:00:00Z"
        );
        Transaction btcReentry = persistTransaction(
            user,
            btc,
            exchange,
            TransactionType.BUY,
            "3.2",
            "3.2",
            "3.8",
            "12.16",
            "2026-03-04T10:00:00Z"
        );
        persistAccumulationTrade(user, ada, adaExit, adaReentry, AccumulationTradeStatus.CLOSED, "1.5", "1.7");
        persistAccumulationTrade(user, btc, btcExit, btcReentry, AccumulationTradeStatus.CLOSED, "3.0", "3.2");

        var rows = accumulationTradeRepository.summarizeByAssetAndStatusAndAssetId(
            user.getId(),
            AccumulationTradeStatus.CLOSED,
            btc.getId()
        );

        assertEquals(1, rows.size());
        assertEquals(btc.getId(), rows.get(0).getAssetId());
        assertEquals(0, rows.get(0).getTotalAccumulationDelta().compareTo(new BigDecimal("0.2")));
    }

    @Test
    void findByUserIdAndSearchMatchesRowsWithoutNullableNullGuard() {
        User user = persistUser("peak-search");
        Asset matchAsset = persistAsset("MATX", "Matching Asset");
        Asset otherAsset = persistAsset("ELSE", "Else Asset");
        Transaction lastBuy = persistTransaction(
            user,
            matchAsset,
            persistExchange("Peak Exchange", "PEX"),
            TransactionType.BUY,
            "1",
            "1",
            "10",
            "10",
            "2026-04-01T10:00:00Z"
        );
        PricePeak triggeredPeak = pricePeakRepository.findByUser_IdAndAsset_Id(user.getId(), matchAsset.getId())
            .orElseThrow();
        triggeredPeak.setPeakPrice(new BigDecimal("11.5"));
        triggeredPeak.setPeakTimestamp(OffsetDateTime.parse("2026-04-02T10:00:00Z"));
        triggeredPeak.setUpdatedAt(OffsetDateTime.parse("2026-04-02T10:00:00Z"));
        entityManager.persistAndFlush(triggeredPeak);
        persistPricePeak(user, otherAsset, null, "2.5", "2026-04-03T10:00:00Z");

        List<PricePeak> rows = pricePeakRepository.findByUser_IdAndSearch(user.getId(), "%mat%");

        assertEquals(1, rows.size());
        assertEquals(matchAsset.getId(), rows.get(0).getAsset().getId());
    }

    private User persistUser(String suffix) {
        User user = new User();
        user.setEmail(suffix + "@example.com");
        user.setUsername(suffix);
        user.setPasswordHash("hash");
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setEnabled(Boolean.TRUE);
        user.setCreatedAt(OffsetDateTime.parse("2026-01-01T00:00:00Z"));
        user.setUpdatedAt(OffsetDateTime.parse("2026-01-01T00:00:00Z"));
        return entityManager.persistAndFlush(user);
    }

    private Asset persistAsset(String symbol, String name) {
        Asset asset = new Asset();
        asset.setSymbol(symbol);
        asset.setName(name);
        return entityManager.persistAndFlush(asset);
    }

    private Exchange persistExchange(String name, String symbol) {
        Exchange exchange = new Exchange();
        exchange.setName(name);
        exchange.setSymbol(symbol);
        return entityManager.persistAndFlush(exchange);
    }

    private Transaction persistTransaction(
        User user,
        Asset asset,
        Exchange exchange,
        TransactionType type,
        String grossAmount,
        String netAmount,
        String unitPriceUsd,
        String totalSpentUsd,
        String transactionDate
    ) {
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setAsset(asset);
        transaction.setExchange(exchange);
        transaction.setTransactionType(type);
        transaction.setGrossAmount(new BigDecimal(grossAmount));
        transaction.setNetAmount(new BigDecimal(netAmount));
        transaction.setUnitPriceUsd(new BigDecimal(unitPriceUsd));
        transaction.setTotalSpentUsd(new BigDecimal(totalSpentUsd));
        transaction.setTransactionDate(OffsetDateTime.parse(transactionDate));
        return entityManager.persistAndFlush(transaction);
    }

    private AccumulationTrade persistAccumulationTrade(
        User user,
        Asset asset,
        Transaction exitTransaction,
        Transaction reentryTransaction,
        AccumulationTradeStatus status,
        String oldCoinAmount,
        String newCoinAmount
    ) {
        AccumulationTrade trade = new AccumulationTrade();
        trade.setUser(user);
        trade.setAsset(asset);
        trade.setExitTransaction(exitTransaction);
        trade.setReentryTransaction(reentryTransaction);
        trade.setStatus(status);
        trade.setOldCoinAmount(new BigDecimal(oldCoinAmount));
        trade.setNewCoinAmount(new BigDecimal(newCoinAmount));
        trade.setExitPriceUsd(exitTransaction.getUnitPriceUsd());
        trade.setReentryPriceUsd(reentryTransaction.getUnitPriceUsd());
        trade.setCreatedAt(OffsetDateTime.parse("2026-02-01T00:00:00Z"));
        trade.setClosedAt(OffsetDateTime.parse("2026-02-02T00:00:00Z"));
        return entityManager.persistAndFlush(trade);
    }

    private PricePeak persistPricePeak(
        User user,
        Asset asset,
        Transaction lastBuyTransaction,
        String peakPrice,
        String peakTimestamp
    ) {
        PricePeak peak = new PricePeak();
        peak.setUser(user);
        peak.setAsset(asset);
        peak.setLastBuyTransaction(lastBuyTransaction);
        peak.setPeakPrice(new BigDecimal(peakPrice));
        peak.setPeakTimestamp(OffsetDateTime.parse(peakTimestamp));
        peak.setActive(Boolean.TRUE);
        peak.setCreatedAt(OffsetDateTime.parse("2026-04-01T00:00:00Z"));
        peak.setUpdatedAt(OffsetDateTime.parse("2026-04-01T00:00:00Z"));
        return entityManager.persistAndFlush(peak);
    }
}
