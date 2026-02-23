package com.trading.service.marketalert;

import com.trading.domain.entity.Asset;
import com.trading.domain.entity.AssetHistoricData;
import com.trading.domain.entity.MarketAlert;
import com.trading.domain.entity.User;
import com.trading.domain.enums.MarketAlertStrategyType;
import com.trading.domain.enums.MarketAlertType;
import com.trading.domain.repository.AssetRepository;
import com.trading.domain.repository.AssetHistoricDataRepository;
import com.trading.domain.repository.MarketAlertRepository;
import com.trading.domain.repository.TransactionRepository;
import com.trading.domain.repository.UserRepository;
import com.trading.dto.marketalert.MarketAlertResponse;
import com.trading.dto.marketalert.MarketScanResponse;
import com.trading.dto.marketalert.MarketSnapshotDTO;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class MarketAlertServiceImpl implements MarketAlertService {

    private static final int FIXED_WINDOW_DAYS = 14;
    private static final int MIN_DYNAMIC_INTERVAL_DAYS = 10;
    private static final String SUMMARY_CACHE_NAME = "marketTechnicalSummary";
    private static final String SUMMARY_CACHE_KEY = "GLOBAL";

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final MathContext MC = MathContext.DECIMAL64;

    private final MarketAlertRepository marketAlertRepository;
    private final TransactionRepository transactionRepository;
    private final AssetRepository assetRepository;
    private final AssetHistoricDataRepository assetHistoricDataRepository;
    private final UserRepository userRepository;
    private final CacheManager cacheManager;

    public MarketAlertServiceImpl(
        MarketAlertRepository marketAlertRepository,
        TransactionRepository transactionRepository,
        AssetRepository assetRepository,
        AssetHistoricDataRepository assetHistoricDataRepository,
        UserRepository userRepository,
        CacheManager cacheManager
    ) {
        this.marketAlertRepository = marketAlertRepository;
        this.transactionRepository = transactionRepository;
        this.assetRepository = assetRepository;
        this.assetHistoricDataRepository = assetHistoricDataRepository;
        this.userRepository = userRepository;
        this.cacheManager = cacheManager;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarketAlertResponse> list(UUID userId) {
        Objects.requireNonNull(userId, "userId is required");
        return marketAlertRepository.findAllByUser_IdOrderByTriggerDateDescCreatedAtDesc(userId).stream()
            .map(MarketAlertServiceImpl::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarketSnapshotDTO> listTechnicalSummary() {
        Cache cache = cacheManager.getCache(SUMMARY_CACHE_NAME);
        if (cache == null) {
            return List.of();
        }
        Cache.ValueWrapper cached = cache.get(SUMMARY_CACHE_KEY);
        if (cached == null || cached.get() == null) {
            return List.of();
        }
        @SuppressWarnings("unchecked")
        List<MarketSnapshotDTO> snapshots = (List<MarketSnapshotDTO>) cached.get();
        return snapshots;
    }

    @Override
    @Transactional
    public MarketScanResponse scan(UUID userId) {
        Objects.requireNonNull(userId, "userId is required");

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        List<Asset> allAssets = assetRepository.findAllByOrderBySymbolAsc();
        Set<UUID> investedAssetIds = transactionRepository.findDistinctInvestedAssetsByUserId(userId)
            .stream()
            .map(Asset::getId)
            .collect(java.util.stream.Collectors.toSet());

        int alertsCreated = 0;
        int fixedAlertsCreated = 0;
        int dynamicAlertsCreated = 0;
        List<MarketSnapshotDTO> snapshots = new java.util.ArrayList<>();

        for (Asset asset : allAssets) {
            // Source of truth for RSI/Stochastic is persisted DB history, never frontend-calculated data.
            List<AssetHistoricData> rows = assetHistoricDataRepository.findAllByAsset_IdOrderByDayDateAsc(asset.getId());
            if (rows.size() < FIXED_WINDOW_DAYS) {
                continue;
            }

            List<AssetHistoricData> lastWindow = rows.subList(rows.size() - FIXED_WINDOW_DAYS, rows.size());
            BigDecimal rsi = calculateRSI(FIXED_WINDOW_DAYS, lastWindow);
            BigDecimal stochastic = calculateStochastic(FIXED_WINDOW_DAYS, lastWindow);
            LocalDate triggerDate = rows.get(rows.size() - 1).getDayDate();
            BigDecimal previousRsi = rows.size() > FIXED_WINDOW_DAYS
                ? calculateRSI(FIXED_WINDOW_DAYS, rows.subList(rows.size() - FIXED_WINDOW_DAYS - 1, rows.size() - 1))
                : rsi;
            snapshots.add(new MarketSnapshotDTO(
                asset.getName(),
                asset.getSymbol(),
                scale4(rsi),
                scale4(stochastic),
                triggerDate,
                resolveMomentum(rsi, previousRsi)
            ));

            if (!investedAssetIds.contains(asset.getId())) {
                continue;
            }

            MarketAlertType alertType = resolveSignalType(rsi, stochastic);
            if (alertType == null) {
                continue;
            }

            boolean fixedCreated = persistIfMissing(
                user,
                asset,
                alertType,
                MarketAlertStrategyType.FIXED_14D,
                rsi,
                stochastic,
                FIXED_WINDOW_DAYS,
                triggerDate
            );
            if (fixedCreated) {
                alertsCreated++;
                fixedAlertsCreated++;
            }

            int dynamicIntervalDays = calculateDynamicIntervalDays(rows, alertType);
            if (dynamicIntervalDays >= MIN_DYNAMIC_INTERVAL_DAYS) {
                boolean dynamicCreated = persistIfMissing(
                    user,
                    asset,
                    alertType,
                    MarketAlertStrategyType.DYNAMIC,
                    rsi,
                    stochastic,
                    dynamicIntervalDays,
                    triggerDate
                );
                if (dynamicCreated) {
                    alertsCreated++;
                    dynamicAlertsCreated++;
                }
            }
        }

        storeSummaryInCache(snapshots);
        return new MarketScanResponse(allAssets.size(), alertsCreated, fixedAlertsCreated, dynamicAlertsCreated, snapshots.size());
    }

    @Override
    @Transactional
    public MarketAlertResponse markViewed(UUID userId, UUID alertId) {
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(alertId, "alertId is required");

        MarketAlert alert = marketAlertRepository.findByIdAndUser_Id(alertId, userId)
            .orElseThrow(() -> new IllegalArgumentException("Market alert not found: " + alertId));

        if (!Boolean.TRUE.equals(alert.getViewed())) {
            alert.setViewed(Boolean.TRUE);
            alert = marketAlertRepository.save(alert);
        }
        return toResponse(alert);
    }

    @Override
    @Transactional
    public void clear(UUID userId) {
        Objects.requireNonNull(userId, "userId is required");
        marketAlertRepository.deleteAllByUser_Id(userId);
    }

    private boolean persistIfMissing(
        User user,
        Asset asset,
        MarketAlertType alertType,
        MarketAlertStrategyType strategyType,
        BigDecimal rsi,
        BigDecimal stochastic,
        int intervalDays,
        LocalDate triggerDate
    ) {
        boolean exists = marketAlertRepository.existsByUser_IdAndAsset_IdAndAlertTypeAndStrategyTypeAndTriggerDate(
            user.getId(),
            asset.getId(),
            alertType,
            strategyType,
            triggerDate
        );
        if (exists) {
            return false;
        }

        MarketAlert alert = new MarketAlert();
        alert.setUser(user);
        alert.setAsset(asset);
        alert.setAlertType(alertType);
        alert.setStrategyType(strategyType);
        alert.setRsiValue(scale4(rsi));
        alert.setStochValue(scale4(stochastic));
        alert.setIntervalDays(intervalDays);
        alert.setTriggerDate(triggerDate);
        alert.setViewed(Boolean.FALSE);
        alert.setCreatedAt(OffsetDateTime.now());
        marketAlertRepository.save(alert);
        return true;
    }

    private static MarketAlertType resolveSignalType(BigDecimal rsi, BigDecimal stochastic) {
        if (rsi.compareTo(new BigDecimal("30")) < 0 && stochastic.compareTo(new BigDecimal("20")) < 0) {
            return MarketAlertType.BUY;
        }
        if (rsi.compareTo(new BigDecimal("70")) > 0 && stochastic.compareTo(new BigDecimal("80")) > 0) {
            return MarketAlertType.SELL;
        }
        return null;
    }

    private static String resolveMomentum(BigDecimal currentRsi, BigDecimal previousRsi) {
        int comparison = currentRsi.compareTo(previousRsi);
        if (comparison > 0) {
            return "UP";
        }
        if (comparison < 0) {
            return "DOWN";
        }
        return "FLAT";
    }

    static BigDecimal calculateStochastic(int days, List<AssetHistoricData> rows) {
        if (rows.size() < days) {
            throw new IllegalArgumentException("Not enough historical rows for stochastic: required " + days);
        }

        List<AssetHistoricData> window = rows.subList(rows.size() - days, rows.size());
        BigDecimal highestHigh = window.stream()
            .map(AssetHistoricData::getHighPrice)
            .max(BigDecimal::compareTo)
            .orElseThrow();
        BigDecimal lowestLow = window.stream()
            .map(AssetHistoricData::getLowPrice)
            .min(BigDecimal::compareTo)
            .orElseThrow();
        BigDecimal currentClose = window.get(window.size() - 1).getClosingPrice();

        BigDecimal denominator = highestHigh.subtract(lowestLow, MC);
        if (denominator.compareTo(ZERO) == 0) {
            return ZERO;
        }

        return currentClose.subtract(lowestLow, MC)
            .divide(denominator, MC)
            .multiply(HUNDRED, MC);
    }

    static BigDecimal calculateRSI(int days, List<AssetHistoricData> rows) {
        if (rows.size() < days) {
            throw new IllegalArgumentException("Not enough historical rows for RSI: required " + days);
        }

        List<AssetHistoricData> window = rows.subList(rows.size() - days, rows.size());
        BigDecimal totalGain = ZERO;
        BigDecimal totalLoss = ZERO;

        for (int i = 1; i < window.size(); i++) {
            BigDecimal change = window.get(i).getClosingPrice().subtract(window.get(i - 1).getClosingPrice(), MC);
            if (change.compareTo(ZERO) > 0) {
                totalGain = totalGain.add(change, MC);
            } else if (change.compareTo(ZERO) < 0) {
                totalLoss = totalLoss.add(change.abs(), MC);
            }
        }

        BigDecimal periods = BigDecimal.valueOf(window.size() - 1L);
        BigDecimal avgGain = totalGain.divide(periods, MC);
        BigDecimal avgLoss = totalLoss.divide(periods, MC);

        if (avgLoss.compareTo(ZERO) == 0) {
            if (avgGain.compareTo(ZERO) == 0) {
                return ZERO;
            }
            return HUNDRED;
        }

        BigDecimal rs = avgGain.divide(avgLoss, MC);
        return HUNDRED.subtract(HUNDRED.divide(ONE.add(rs, MC), MC), MC);
    }

    private static int calculateDynamicIntervalDays(List<AssetHistoricData> orderedRows, MarketAlertType alertType) {
        int latestIndex = orderedRows.size() - 1;
        for (int i = latestIndex; i >= 1; i--) {
            BigDecimal currentClose = orderedRows.get(i).getClosingPrice();
            BigDecimal previousClose = orderedRows.get(i - 1).getClosingPrice();
            int compare = currentClose.compareTo(previousClose);

            if (alertType == MarketAlertType.BUY && compare > 0) {
                return latestIndex - i + 1;
            }
            if (alertType == MarketAlertType.SELL && compare < 0) {
                return latestIndex - i + 1;
            }
        }
        return orderedRows.size();
    }

    private static BigDecimal scale4(BigDecimal value) {
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    private void storeSummaryInCache(List<MarketSnapshotDTO> snapshots) {
        Cache cache = cacheManager.getCache(SUMMARY_CACHE_NAME);
        if (cache != null) {
            cache.put(SUMMARY_CACHE_KEY, List.copyOf(snapshots));
        }
    }

    private static MarketAlertResponse toResponse(MarketAlert alert) {
        return new MarketAlertResponse(
            alert.getId(),
            alert.getAsset().getId(),
            alert.getAsset().getSymbol(),
            alert.getAsset().getName(),
            alert.getAlertType(),
            alert.getStrategyType(),
            alert.getRsiValue(),
            alert.getStochValue(),
            alert.getIntervalDays(),
            alert.getTriggerDate(),
            alert.getViewed(),
            alert.getCreatedAt()
        );
    }
}
