package com.trading.service.backup;

import com.trading.domain.entity.AccumulationTrade;
import com.trading.domain.entity.Asset;
import com.trading.domain.entity.AssetHistoricData;
import com.trading.domain.entity.BuyStrategy;
import com.trading.domain.entity.Exchange;
import com.trading.domain.entity.MarketAlert;
import com.trading.domain.entity.PricePeak;
import com.trading.domain.entity.SellStrategy;
import com.trading.domain.entity.StrategyAlert;
import com.trading.domain.entity.Transaction;
import com.trading.domain.entity.User;
import com.trading.domain.entity.UserPreference;
import com.trading.domain.enums.AccumulationTradeStatus;
import com.trading.domain.enums.AuthProvider;
import com.trading.domain.enums.BuyInputMode;
import com.trading.domain.enums.MarketAlertStrategyType;
import com.trading.domain.enums.MarketAlertType;
import com.trading.domain.enums.StrategyAlertStatus;
import com.trading.domain.enums.StrategyType;
import com.trading.domain.enums.TransactionType;
import com.trading.domain.repository.AccumulationTradeRepository;
import com.trading.domain.repository.AssetHistoricDataRepository;
import com.trading.domain.repository.AssetRepository;
import com.trading.domain.repository.BuyStrategyRepository;
import com.trading.domain.repository.ExchangeRepository;
import com.trading.domain.repository.MarketAlertRepository;
import com.trading.domain.repository.PricePeakRepository;
import com.trading.domain.repository.SellStrategyRepository;
import com.trading.domain.repository.StrategyAlertRepository;
import com.trading.domain.repository.TransactionRepository;
import com.trading.domain.repository.UserPreferenceRepository;
import com.trading.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackupServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserPreferenceRepository userPreferenceRepository;
    @Mock
    private AssetRepository assetRepository;
    @Mock
    private ExchangeRepository exchangeRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private BuyStrategyRepository buyStrategyRepository;
    @Mock
    private SellStrategyRepository sellStrategyRepository;
    @Mock
    private PricePeakRepository pricePeakRepository;
    @Mock
    private StrategyAlertRepository strategyAlertRepository;
    @Mock
    private MarketAlertRepository marketAlertRepository;
    @Mock
    private AssetHistoricDataRepository assetHistoricDataRepository;
    @Mock
    private AccumulationTradeRepository accumulationTradeRepository;

    @InjectMocks
    private BackupServiceImpl backupService;

    @Test
    void writeUserBackupSqlProducesFkSafeOrderedSqlWithFormatting() throws Exception {
        UUID userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID assetId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID exchangeId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID txId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        UUID preferenceId = UUID.fromString("44444444-4444-4444-4444-444444444444");

        User user = new User();
        user.setId(userId);
        user.setEmail("user.o'hara@example.com");
        user.setUsername("user");
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setEnabled(Boolean.TRUE);
        user.setCreatedAt(OffsetDateTime.of(2026, 2, 20, 10, 15, 30, 0, ZoneOffset.UTC));
        user.setUpdatedAt(OffsetDateTime.of(2026, 2, 21, 11, 0, 0, 0, ZoneOffset.UTC));

        Asset asset = new Asset();
        asset.setId(assetId);
        asset.setSymbol("BTC");
        asset.setName("Bitcoin");
        asset.setCoinGeckoId("bitcoin");

        Exchange exchange = new Exchange();
        exchange.setId(exchangeId);
        exchange.setName("Binance");
        exchange.setSymbol("BIN");

        UserPreference userPreference = new UserPreference();
        userPreference.setId(preferenceId);
        userPreference.setUser(user);
        userPreference.setDefaultBuyInputMode(BuyInputMode.COIN_AMOUNT);
        userPreference.setCreatedAt(OffsetDateTime.of(2026, 2, 20, 10, 15, 30, 0, ZoneOffset.UTC));
        userPreference.setUpdatedAt(OffsetDateTime.of(2026, 2, 21, 11, 0, 0, 0, ZoneOffset.UTC));

        Transaction transaction = new Transaction();
        transaction.setId(txId);
        transaction.setUser(user);
        transaction.setAsset(asset);
        transaction.setExchange(exchange);
        transaction.setTransactionType(TransactionType.BUY);
        transaction.setGrossAmount(new BigDecimal("0.12340000"));
        transaction.setFeeAmount(new BigDecimal("0.00010000"));
        transaction.setFeePercentage(new BigDecimal("0.250000"));
        transaction.setFeeCurrency("USD");
        transaction.setNetAmount(new BigDecimal("0.12330000"));
        transaction.setUnitPriceUsd(new BigDecimal("50000.12000000"));
        transaction.setTotalSpentUsd(new BigDecimal("6165.01479600"));
        transaction.setRealizedPnl(null);
        transaction.setTransactionDate(OffsetDateTime.of(2026, 2, 20, 12, 30, 45, 0, ZoneOffset.UTC));

        BuyStrategy buyStrategy = new BuyStrategy();
        buyStrategy.setId(UUID.fromString("55555555-5555-5555-5555-555555555555"));
        buyStrategy.setAsset(asset);
        buyStrategy.setDipThresholdPercent(new BigDecimal("5.50"));
        buyStrategy.setBuyAmountUsd(new BigDecimal("1000.00"));
        buyStrategy.setActive(Boolean.TRUE);
        buyStrategy.setCreatedAt(OffsetDateTime.of(2026, 2, 20, 12, 0, 0, 0, ZoneOffset.UTC));
        buyStrategy.setUpdatedAt(OffsetDateTime.of(2026, 2, 20, 12, 0, 0, 0, ZoneOffset.UTC));
        buyStrategy.setUser(user);

        SellStrategy sellStrategy = new SellStrategy();
        sellStrategy.setId(UUID.fromString("66666666-6666-6666-6666-666666666666"));
        sellStrategy.setAsset(asset);
        sellStrategy.setThresholdPercent(new BigDecimal("7.25"));
        sellStrategy.setActive(Boolean.FALSE);
        sellStrategy.setCreatedAt(OffsetDateTime.of(2026, 2, 20, 12, 0, 0, 0, ZoneOffset.UTC));
        sellStrategy.setUpdatedAt(OffsetDateTime.of(2026, 2, 20, 12, 0, 0, 0, ZoneOffset.UTC));
        sellStrategy.setUser(user);

        PricePeak pricePeak = new PricePeak();
        pricePeak.setId(UUID.fromString("77777777-7777-7777-7777-777777777777"));
        pricePeak.setAsset(asset);
        pricePeak.setLastBuyTransaction(transaction);
        pricePeak.setPeakPrice(new BigDecimal("52000.12"));
        pricePeak.setPeakTimestamp(OffsetDateTime.of(2026, 2, 20, 12, 35, 0, 0, ZoneOffset.UTC));
        pricePeak.setActive(Boolean.TRUE);
        pricePeak.setCreatedAt(OffsetDateTime.of(2026, 2, 20, 12, 35, 0, 0, ZoneOffset.UTC));
        pricePeak.setUpdatedAt(OffsetDateTime.of(2026, 2, 20, 12, 35, 0, 0, ZoneOffset.UTC));
        pricePeak.setUser(user);

        StrategyAlert strategyAlert = new StrategyAlert();
        strategyAlert.setId(UUID.fromString("88888888-8888-8888-8888-888888888888"));
        strategyAlert.setAsset(asset);
        strategyAlert.setStrategyType(StrategyType.BUY);
        strategyAlert.setTriggerPrice(new BigDecimal("45000.00"));
        strategyAlert.setThresholdPercent(new BigDecimal("10.00"));
        strategyAlert.setReferencePrice(new BigDecimal("50000.00"));
        strategyAlert.setAlertMessage("Buy signal");
        strategyAlert.setStatus(StrategyAlertStatus.PENDING);
        strategyAlert.setCreatedAt(OffsetDateTime.of(2026, 2, 20, 13, 0, 0, 0, ZoneOffset.UTC));
        strategyAlert.setUser(user);

        MarketAlert marketAlert = new MarketAlert();
        marketAlert.setId(UUID.fromString("99999999-9999-9999-9999-999999999999"));
        marketAlert.setUser(user);
        marketAlert.setAsset(asset);
        marketAlert.setAlertType(MarketAlertType.BUY);
        marketAlert.setStrategyType(MarketAlertStrategyType.FIXED_14D);
        marketAlert.setRsiValue(new BigDecimal("30.2500"));
        marketAlert.setStochValue(new BigDecimal("20.1200"));
        marketAlert.setIntervalDays(14);
        marketAlert.setTriggerDate(LocalDate.of(2026, 2, 20));
        marketAlert.setViewed(Boolean.FALSE);
        marketAlert.setCreatedAt(OffsetDateTime.of(2026, 2, 20, 13, 30, 0, 0, ZoneOffset.UTC));

        AssetHistoricData historicData = new AssetHistoricData();
        historicData.setId(UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"));
        historicData.setAsset(asset);
        historicData.setDayDate(LocalDate.of(2026, 2, 19));
        historicData.setHighPrice(new BigDecimal("51000.11"));
        historicData.setLowPrice(new BigDecimal("49000.22"));
        historicData.setClosingPrice(new BigDecimal("50000.33"));
        historicData.setCreatedAt(OffsetDateTime.of(2026, 2, 19, 23, 59, 59, 0, ZoneOffset.UTC));

        AccumulationTrade accumulationTrade = new AccumulationTrade();
        accumulationTrade.setId(UUID.fromString("bbbbbbbb-cccc-dddd-eeee-ffffffffffff"));
        accumulationTrade.setExitTransaction(transaction);
        accumulationTrade.setReentryTransaction(null);
        accumulationTrade.setAsset(asset);
        accumulationTrade.setOldCoinAmount(new BigDecimal("0.1200"));
        accumulationTrade.setNewCoinAmount(null);
        accumulationTrade.setStatus(AccumulationTradeStatus.OPEN);
        accumulationTrade.setExitPriceUsd(new BigDecimal("51000.11"));
        accumulationTrade.setReentryPriceUsd(null);
        accumulationTrade.setCreatedAt(OffsetDateTime.of(2026, 2, 20, 14, 0, 0, 0, ZoneOffset.UTC));
        accumulationTrade.setClosedAt(null);
        accumulationTrade.setPredictionNotes("waiting");
        accumulationTrade.setUser(user);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userPreferenceRepository.findByUser_Id(userId)).thenReturn(Optional.of(userPreference));
        when(transactionRepository.findAllByUser_IdOrderByTransactionDateDesc(userId)).thenReturn(List.of(transaction));
        when(buyStrategyRepository.findAllByUser_IdOrderByUpdatedAtDesc(userId)).thenReturn(List.of(buyStrategy));
        when(sellStrategyRepository.findAllByUser_IdOrderByUpdatedAtDesc(userId)).thenReturn(List.of(sellStrategy));
        when(pricePeakRepository.findAllByUser_IdOrderByUpdatedAtDesc(userId)).thenReturn(List.of(pricePeak));
        when(strategyAlertRepository.findAllByUser_IdOrderByCreatedAtDesc(userId)).thenReturn(List.of(strategyAlert));
        when(marketAlertRepository.findAllByUser_IdOrderByTriggerDateDescCreatedAtDesc(userId)).thenReturn(List.of(marketAlert));
        when(assetHistoricDataRepository.findAllForUserInvestedAssets(userId)).thenReturn(List.of(historicData));
        when(accumulationTradeRepository.findAllByUser_IdOrderByCreatedAtDesc(userId)).thenReturn(List.of(accumulationTrade));
        when(assetRepository.findAllById(any())).thenReturn(List.of(asset));
        when(exchangeRepository.findAllById(any())).thenReturn(List.of(exchange));

        StringWriter writer = new StringWriter();
        backupService.writeUserBackupSql(userId, writer);
        String sql = writer.toString();

        assertTrue(sql.startsWith("SET REFERENTIAL_INTEGRITY FALSE;\n"));
        assertTrue(sql.contains("SET REFERENTIAL_INTEGRITY TRUE;\n"));
        assertTrue(sql.contains("INSERT INTO users"));
        assertTrue(sql.contains("INSERT INTO assets"));
        assertTrue(sql.contains("INSERT INTO exchanges"));
        assertTrue(sql.contains("INSERT INTO transactions"));
        assertTrue(sql.contains("'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'"));
        assertTrue(sql.contains("'2026-02-20 12:30:45'"));
        assertTrue(sql.contains("50000.12000000"));
        assertTrue(sql.contains("TRUE"));
        assertTrue(sql.contains("FALSE"));
        assertTrue(sql.contains("user.o''hara@example.com"));

        int usersPos = sql.indexOf("INSERT INTO users");
        int assetsPos = sql.indexOf("INSERT INTO assets");
        int exchangesPos = sql.indexOf("INSERT INTO exchanges");
        int txPos = sql.indexOf("INSERT INTO transactions");
        int accumPos = sql.indexOf("INSERT INTO accumulation_trades");
        assertTrue(usersPos < assetsPos);
        assertTrue(assetsPos < exchangesPos);
        assertTrue(exchangesPos < txPos);
        assertTrue(txPos < accumPos);
    }
}
