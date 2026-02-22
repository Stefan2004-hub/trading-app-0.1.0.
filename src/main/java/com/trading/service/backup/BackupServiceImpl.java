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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BackupServiceImpl implements BackupService {

    private static final String LINE_SEPARATOR = "\n";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserRepository userRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final AssetRepository assetRepository;
    private final ExchangeRepository exchangeRepository;
    private final TransactionRepository transactionRepository;
    private final BuyStrategyRepository buyStrategyRepository;
    private final SellStrategyRepository sellStrategyRepository;
    private final PricePeakRepository pricePeakRepository;
    private final StrategyAlertRepository strategyAlertRepository;
    private final MarketAlertRepository marketAlertRepository;
    private final AssetHistoricDataRepository assetHistoricDataRepository;
    private final AccumulationTradeRepository accumulationTradeRepository;

    public BackupServiceImpl(
        UserRepository userRepository,
        UserPreferenceRepository userPreferenceRepository,
        AssetRepository assetRepository,
        ExchangeRepository exchangeRepository,
        TransactionRepository transactionRepository,
        BuyStrategyRepository buyStrategyRepository,
        SellStrategyRepository sellStrategyRepository,
        PricePeakRepository pricePeakRepository,
        StrategyAlertRepository strategyAlertRepository,
        MarketAlertRepository marketAlertRepository,
        AssetHistoricDataRepository assetHistoricDataRepository,
        AccumulationTradeRepository accumulationTradeRepository
    ) {
        this.userRepository = userRepository;
        this.userPreferenceRepository = userPreferenceRepository;
        this.assetRepository = assetRepository;
        this.exchangeRepository = exchangeRepository;
        this.transactionRepository = transactionRepository;
        this.buyStrategyRepository = buyStrategyRepository;
        this.sellStrategyRepository = sellStrategyRepository;
        this.pricePeakRepository = pricePeakRepository;
        this.strategyAlertRepository = strategyAlertRepository;
        this.marketAlertRepository = marketAlertRepository;
        this.assetHistoricDataRepository = assetHistoricDataRepository;
        this.accumulationTradeRepository = accumulationTradeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public void writeUserBackupSql(UUID userId, Writer writer) throws IOException {
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(writer, "writer is required");

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        UserPreference userPreference = userPreferenceRepository.findByUser_Id(userId).orElse(null);

        List<Transaction> transactions = transactionRepository.findAllByUser_IdOrderByTransactionDateDesc(userId)
            .stream()
            .sorted(Comparator.comparing(Transaction::getTransactionDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Transaction::getId))
            .toList();
        List<BuyStrategy> buyStrategies = new ArrayList<>(buyStrategyRepository.findAllByUser_IdOrderByUpdatedAtDesc(userId));
        List<SellStrategy> sellStrategies = new ArrayList<>(sellStrategyRepository.findAllByUser_IdOrderByUpdatedAtDesc(userId));
        List<PricePeak> pricePeaks = new ArrayList<>(pricePeakRepository.findAllByUser_IdOrderByUpdatedAtDesc(userId));
        List<StrategyAlert> strategyAlerts = new ArrayList<>(strategyAlertRepository.findAllByUser_IdOrderByCreatedAtDesc(userId));
        List<MarketAlert> marketAlerts = new ArrayList<>(marketAlertRepository.findAllByUser_IdOrderByTriggerDateDescCreatedAtDesc(userId));
        List<AssetHistoricData> assetHistoricData = new ArrayList<>(assetHistoricDataRepository.findAllForUserInvestedAssets(userId));
        List<AccumulationTrade> accumulationTrades = new ArrayList<>(accumulationTradeRepository.findAllByUser_IdOrderByCreatedAtDesc(userId));

        sortEntitiesById(buyStrategies, BuyStrategy::getId);
        sortEntitiesById(sellStrategies, SellStrategy::getId);
        sortEntitiesById(pricePeaks, PricePeak::getId);
        sortEntitiesById(strategyAlerts, StrategyAlert::getId);
        sortEntitiesById(marketAlerts, MarketAlert::getId);
        sortEntitiesById(accumulationTrades, AccumulationTrade::getId);
        assetHistoricData.sort(
            Comparator.comparing(AssetHistoricData::getDayDate)
                .thenComparing(row -> row.getAsset().getId())
                .thenComparing(AssetHistoricData::getId)
        );

        Set<UUID> assetIds = new LinkedHashSet<>();
        Set<UUID> exchangeIds = new LinkedHashSet<>();

        collectIdsFromTransactions(transactions, assetIds, exchangeIds);
        collectAssetIds(buyStrategies.stream().map(BuyStrategy::getAsset).toList(), assetIds);
        collectAssetIds(sellStrategies.stream().map(SellStrategy::getAsset).toList(), assetIds);
        collectAssetIds(pricePeaks.stream().map(PricePeak::getAsset).toList(), assetIds);
        collectAssetIds(strategyAlerts.stream().map(StrategyAlert::getAsset).toList(), assetIds);
        collectAssetIds(marketAlerts.stream().map(MarketAlert::getAsset).toList(), assetIds);
        collectAssetIds(assetHistoricData.stream().map(AssetHistoricData::getAsset).toList(), assetIds);
        for (AccumulationTrade accumulationTrade : accumulationTrades) {
            if (accumulationTrade.getAsset() != null) {
                assetIds.add(accumulationTrade.getAsset().getId());
            }
            includeTransactionLookupIds(accumulationTrade.getExitTransaction(), assetIds, exchangeIds);
            includeTransactionLookupIds(accumulationTrade.getReentryTransaction(), assetIds, exchangeIds);
        }
        for (PricePeak pricePeak : pricePeaks) {
            includeTransactionLookupIds(pricePeak.getLastBuyTransaction(), assetIds, exchangeIds);
        }

        List<Asset> assets = assetRepository.findAllById(assetIds)
            .stream()
            .sorted(Comparator.comparing(Asset::getSymbol, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(Asset::getId))
            .toList();
        List<Exchange> exchanges = exchangeRepository.findAllById(exchangeIds)
            .stream()
            .sorted(Comparator.comparing(Exchange::getName, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(Exchange::getId))
            .toList();

        writer.write("SET REFERENTIAL_INTEGRITY FALSE;");
        writer.write(LINE_SEPARATOR);

        writeUserInserts(writer, List.of(user));
        writeAssetInserts(writer, assets);
        writeExchangeInserts(writer, exchanges);
        writeUserPreferenceInserts(writer, userPreference == null ? List.of() : List.of(userPreference));
        writeTransactionInserts(writer, transactions);
        writeBuyStrategyInserts(writer, buyStrategies);
        writeSellStrategyInserts(writer, sellStrategies);
        writePricePeakInserts(writer, pricePeaks);
        writeStrategyAlertInserts(writer, strategyAlerts);
        writeMarketAlertInserts(writer, marketAlerts);
        writeAssetHistoricDataInserts(writer, assetHistoricData);
        writeAccumulationTradeInserts(writer, accumulationTrades);

        writer.write("SET REFERENTIAL_INTEGRITY TRUE;");
        writer.write(LINE_SEPARATOR);
        writer.flush();
    }

    private static <T> void sortEntitiesById(List<T> entities, java.util.function.Function<T, UUID> idAccessor) {
        entities.sort(Comparator.comparing(idAccessor));
    }

    private static void collectIdsFromTransactions(
        List<Transaction> transactions,
        Set<UUID> assetIds,
        Set<UUID> exchangeIds
    ) {
        for (Transaction transaction : transactions) {
            includeTransactionLookupIds(transaction, assetIds, exchangeIds);
        }
    }

    private static void collectAssetIds(Collection<Asset> assets, Set<UUID> assetIds) {
        for (Asset asset : assets) {
            if (asset != null && asset.getId() != null) {
                assetIds.add(asset.getId());
            }
        }
    }

    private static void includeTransactionLookupIds(
        Transaction transaction,
        Set<UUID> assetIds,
        Set<UUID> exchangeIds
    ) {
        if (transaction == null) {
            return;
        }
        if (transaction.getAsset() != null && transaction.getAsset().getId() != null) {
            assetIds.add(transaction.getAsset().getId());
        }
        if (transaction.getExchange() != null && transaction.getExchange().getId() != null) {
            exchangeIds.add(transaction.getExchange().getId());
        }
    }

    private static void writeUserInserts(Writer writer, List<User> users) throws IOException {
        for (User user : users) {
            writeInsert(writer, "users", orderedMap(
                "id", user.getId(),
                "email", user.getEmail(),
                "username", user.getUsername(),
                "password_hash", user.getPasswordHash(),
                "auth_provider", user.getAuthProvider() == null ? null : user.getAuthProvider().name(),
                "is_enabled", user.getEnabled(),
                "created_at", user.getCreatedAt(),
                "updated_at", user.getUpdatedAt()
            ));
        }
    }

    private static void writeAssetInserts(Writer writer, List<Asset> assets) throws IOException {
        for (Asset asset : assets) {
            writeInsert(writer, "assets", orderedMap(
                "id", asset.getId(),
                "symbol", asset.getSymbol(),
                "name", asset.getName(),
                "coin_gecko_id", asset.getCoinGeckoId()
            ));
        }
    }

    private static void writeExchangeInserts(Writer writer, List<Exchange> exchanges) throws IOException {
        for (Exchange exchange : exchanges) {
            writeInsert(writer, "exchanges", orderedMap(
                "id", exchange.getId(),
                "name", exchange.getName(),
                "symbol", exchange.getSymbol()
            ));
        }
    }

    private static void writeUserPreferenceInserts(Writer writer, List<UserPreference> preferences) throws IOException {
        for (UserPreference preference : preferences) {
            writeInsert(writer, "user_preferences", orderedMap(
                "id", preference.getId(),
                "user_id", preference.getUser() == null ? null : preference.getUser().getId(),
                "default_buy_input_mode", preference.getDefaultBuyInputMode() == null ? null : preference.getDefaultBuyInputMode().name(),
                "created_at", preference.getCreatedAt(),
                "updated_at", preference.getUpdatedAt()
            ));
        }
    }

    private static void writeTransactionInserts(Writer writer, List<Transaction> transactions) throws IOException {
        for (Transaction transaction : transactions) {
            writeInsert(writer, "transactions", orderedMap(
                "id", transaction.getId(),
                "asset_id", transaction.getAsset() == null ? null : transaction.getAsset().getId(),
                "exchange_id", transaction.getExchange() == null ? null : transaction.getExchange().getId(),
                "transaction_type", transaction.getTransactionType() == null ? null : transaction.getTransactionType().name(),
                "gross_amount", transaction.getGrossAmount(),
                "fee_amount", transaction.getFeeAmount(),
                "fee_percentage", transaction.getFeePercentage(),
                "fee_currency", transaction.getFeeCurrency(),
                "net_amount", transaction.getNetAmount(),
                "unit_price_usd", transaction.getUnitPriceUsd(),
                "total_spent_usd", transaction.getTotalSpentUsd(),
                "realized_pnl", transaction.getRealizedPnl(),
                "user_id", transaction.getUser() == null ? null : transaction.getUser().getId(),
                "transaction_date", transaction.getTransactionDate()
            ));
        }
    }

    private static void writeBuyStrategyInserts(Writer writer, List<BuyStrategy> buyStrategies) throws IOException {
        for (BuyStrategy strategy : buyStrategies) {
            writeInsert(writer, "buy_strategies", orderedMap(
                "id", strategy.getId(),
                "asset_id", strategy.getAsset() == null ? null : strategy.getAsset().getId(),
                "dip_threshold_percent", strategy.getDipThresholdPercent(),
                "buy_amount_usd", strategy.getBuyAmountUsd(),
                "is_active", strategy.getActive(),
                "created_at", strategy.getCreatedAt(),
                "updated_at", strategy.getUpdatedAt(),
                "user_id", strategy.getUser() == null ? null : strategy.getUser().getId()
            ));
        }
    }

    private static void writeSellStrategyInserts(Writer writer, List<SellStrategy> sellStrategies) throws IOException {
        for (SellStrategy strategy : sellStrategies) {
            writeInsert(writer, "sell_strategies", orderedMap(
                "id", strategy.getId(),
                "asset_id", strategy.getAsset() == null ? null : strategy.getAsset().getId(),
                "threshold_percent", strategy.getThresholdPercent(),
                "is_active", strategy.getActive(),
                "created_at", strategy.getCreatedAt(),
                "updated_at", strategy.getUpdatedAt(),
                "user_id", strategy.getUser() == null ? null : strategy.getUser().getId()
            ));
        }
    }

    private static void writePricePeakInserts(Writer writer, List<PricePeak> pricePeaks) throws IOException {
        for (PricePeak peak : pricePeaks) {
            writeInsert(writer, "price_peaks", orderedMap(
                "id", peak.getId(),
                "asset_id", peak.getAsset() == null ? null : peak.getAsset().getId(),
                "last_buy_transaction_id", peak.getLastBuyTransaction() == null ? null : peak.getLastBuyTransaction().getId(),
                "peak_price", peak.getPeakPrice(),
                "peak_timestamp", peak.getPeakTimestamp(),
                "is_active", peak.getActive(),
                "created_at", peak.getCreatedAt(),
                "updated_at", peak.getUpdatedAt(),
                "user_id", peak.getUser() == null ? null : peak.getUser().getId()
            ));
        }
    }

    private static void writeStrategyAlertInserts(Writer writer, List<StrategyAlert> strategyAlerts) throws IOException {
        for (StrategyAlert alert : strategyAlerts) {
            writeInsert(writer, "strategy_alerts", orderedMap(
                "id", alert.getId(),
                "asset_id", alert.getAsset() == null ? null : alert.getAsset().getId(),
                "strategy_type", alert.getStrategyType() == null ? null : alert.getStrategyType().name(),
                "trigger_price", alert.getTriggerPrice(),
                "threshold_percent", alert.getThresholdPercent(),
                "reference_price", alert.getReferencePrice(),
                "alert_message", alert.getAlertMessage(),
                "status", alert.getStatus() == null ? null : alert.getStatus().name(),
                "acknowledged_at", alert.getAcknowledgedAt(),
                "executed_at", alert.getExecutedAt(),
                "created_at", alert.getCreatedAt(),
                "user_id", alert.getUser() == null ? null : alert.getUser().getId()
            ));
        }
    }

    private static void writeMarketAlertInserts(Writer writer, List<MarketAlert> marketAlerts) throws IOException {
        for (MarketAlert alert : marketAlerts) {
            writeInsert(writer, "market_alerts", orderedMap(
                "id", alert.getId(),
                "user_id", alert.getUser() == null ? null : alert.getUser().getId(),
                "asset_id", alert.getAsset() == null ? null : alert.getAsset().getId(),
                "alert_type", alert.getAlertType() == null ? null : alert.getAlertType().name(),
                "strategy_type", alert.getStrategyType() == null ? null : alert.getStrategyType().name(),
                "rsi_value", alert.getRsiValue(),
                "stoch_value", alert.getStochValue(),
                "interval_days", alert.getIntervalDays(),
                "trigger_date", alert.getTriggerDate(),
                "is_viewed", alert.getViewed(),
                "created_at", alert.getCreatedAt()
            ));
        }
    }

    private static void writeAssetHistoricDataInserts(Writer writer, List<AssetHistoricData> rows) throws IOException {
        for (AssetHistoricData row : rows) {
            writeInsert(writer, "asset_historic_data", orderedMap(
                "id", row.getId(),
                "asset_id", row.getAsset() == null ? null : row.getAsset().getId(),
                "day_date", row.getDayDate(),
                "high_price", row.getHighPrice(),
                "low_price", row.getLowPrice(),
                "closing_price", row.getClosingPrice(),
                "created_at", row.getCreatedAt()
            ));
        }
    }

    private static void writeAccumulationTradeInserts(Writer writer, List<AccumulationTrade> accumulationTrades) throws IOException {
        for (AccumulationTrade trade : accumulationTrades) {
            writeInsert(writer, "accumulation_trades", orderedMap(
                "id", trade.getId(),
                "exit_transaction_id", trade.getExitTransaction() == null ? null : trade.getExitTransaction().getId(),
                "reentry_transaction_id", trade.getReentryTransaction() == null ? null : trade.getReentryTransaction().getId(),
                "asset_id", trade.getAsset() == null ? null : trade.getAsset().getId(),
                "old_coin_amount", trade.getOldCoinAmount(),
                "new_coin_amount", trade.getNewCoinAmount(),
                "status", trade.getStatus() == null ? null : trade.getStatus().name(),
                "exit_price_usd", trade.getExitPriceUsd(),
                "reentry_price_usd", trade.getReentryPriceUsd(),
                "created_at", trade.getCreatedAt(),
                "closed_at", trade.getClosedAt(),
                "prediction_notes", trade.getPredictionNotes(),
                "user_id", trade.getUser() == null ? null : trade.getUser().getId()
            ));
        }
    }

    private static void writeInsert(Writer writer, String tableName, LinkedHashMap<String, Object> row) throws IOException {
        String columns = String.join(", ", row.keySet());
        String values = row.values().stream().map(BackupServiceImpl::toSqlLiteral).collect(Collectors.joining(", "));
        writer.write("INSERT INTO " + tableName + " (" + columns + ") VALUES (" + values + ");");
        writer.write(LINE_SEPARATOR);
    }

    private static LinkedHashMap<String, Object> orderedMap(Object... pairs) {
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("pairs must be key-value entries");
        }
        LinkedHashMap<String, Object> ordered = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            String key = (String) pairs[i];
            Object value = pairs[i + 1];
            ordered.put(key, value);
        }
        return ordered;
    }

    private static String toSqlLiteral(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof UUID uuid) {
            return quote(uuid.toString());
        }
        if (value instanceof String str) {
            return quote(str);
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.toPlainString();
        }
        if (value instanceof Boolean bool) {
            return bool ? "TRUE" : "FALSE";
        }
        if (value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Double || value instanceof Float) {
            return String.valueOf(value);
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            LocalDateTime normalized = offsetDateTime.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
            return quote(DATE_TIME_FORMATTER.format(normalized));
        }
        if (value instanceof LocalDateTime localDateTime) {
            return quote(DATE_TIME_FORMATTER.format(localDateTime));
        }
        if (value instanceof LocalDate localDate) {
            return quote(localDate.toString());
        }
        if (value instanceof Enum<?> enumValue) {
            return quote(enumValue.name());
        }

        return quote(String.valueOf(value));
    }

    private static String quote(String value) {
        return "'" + value.replace("'", "''") + "'";
    }
}
