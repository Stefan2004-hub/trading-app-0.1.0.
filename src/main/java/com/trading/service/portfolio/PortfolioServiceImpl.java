package com.trading.service.portfolio;

import com.trading.domain.projection.UserAssetLatestPriceProjection;
import com.trading.domain.projection.UserAssetRealizedPnlProjection;
import com.trading.domain.projection.UserPortfolioPerformanceProjection;
import com.trading.domain.repository.TransactionRepository;
import com.trading.dto.portfolio.PortfolioAssetPerformanceResponse;
import com.trading.dto.portfolio.PortfolioSummaryResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PortfolioServiceImpl implements PortfolioService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final TransactionRepository transactionRepository;

    public PortfolioServiceImpl(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public PortfolioSummaryResponse getSummary(UUID userId) {
        Objects.requireNonNull(userId, "userId is required");
        List<PortfolioAssetPerformanceResponse> performance = getPerformance(userId);

        BigDecimal totalInvested = performance.stream()
            .map(PortfolioAssetPerformanceResponse::totalInvestedUsd)
            .reduce(ZERO, BigDecimal::add);
        BigDecimal totalCurrentValue = performance.stream()
            .map(PortfolioAssetPerformanceResponse::currentValueUsd)
            .reduce(ZERO, BigDecimal::add);
        BigDecimal totalUnrealized = performance.stream()
            .map(PortfolioAssetPerformanceResponse::unrealizedPnlUsd)
            .reduce(ZERO, BigDecimal::add);
        BigDecimal totalRealized = performance.stream()
            .map(PortfolioAssetPerformanceResponse::realizedPnlUsd)
            .reduce(ZERO, BigDecimal::add);
        BigDecimal totalPnl = totalUnrealized.add(totalRealized);
        BigDecimal totalUnrealizedPercent = percentage(totalUnrealized, totalInvested);

        return new PortfolioSummaryResponse(
            totalInvested,
            totalCurrentValue,
            totalUnrealized,
            totalUnrealizedPercent,
            totalRealized,
            totalPnl,
            performance
        );
    }

    @Override
    public List<PortfolioAssetPerformanceResponse> getPerformance(UUID userId) {
        Objects.requireNonNull(userId, "userId is required");

        List<UserPortfolioPerformanceProjection> rows = transactionRepository.findPortfolioPerformanceByUserId(userId);
        Map<String, BigDecimal> latestPrices = transactionRepository.findLatestUnitPricesByUserId(userId).stream()
            .collect(
                Collectors.toMap(
                    row -> key(row.getSymbol(), row.getExchange()),
                    UserAssetLatestPriceProjection::getLatestUnitPriceUsd,
                    (first, ignored) -> first
                )
            );
        Map<String, BigDecimal> realizedPnlByAsset = transactionRepository.findRealizedPnlByUserId(userId).stream()
            .collect(
                Collectors.toMap(
                    row -> key(row.getSymbol(), row.getExchange()),
                    UserAssetRealizedPnlProjection::getRealizedPnl,
                    BigDecimal::add
                )
            );

        return rows.stream()
            .map(row -> toPerformanceRow(row, latestPrices, realizedPnlByAsset))
            .toList();
    }

    private static PortfolioAssetPerformanceResponse toPerformanceRow(
        UserPortfolioPerformanceProjection row,
        Map<String, BigDecimal> latestPrices,
        Map<String, BigDecimal> realizedPnlByAsset
    ) {
        BigDecimal currentBalance = nullSafe(row.getCurrentBalance());
        BigDecimal totalInvested = nullSafe(row.getTotalInvestedUsd());
        BigDecimal avgBuyPrice = nullSafe(row.getAvgBuyPrice());

        String assetKey = key(row.getSymbol(), row.getExchange());
        BigDecimal currentUnitPrice = latestPrices.getOrDefault(assetKey, avgBuyPrice);
        BigDecimal currentValue = currentBalance.multiply(currentUnitPrice);
        BigDecimal unrealizedPnl = currentValue.subtract(totalInvested);
        BigDecimal unrealizedPnlPercent = percentage(unrealizedPnl, totalInvested);
        BigDecimal realizedPnl = nullSafe(realizedPnlByAsset.get(assetKey));
        BigDecimal totalPnl = unrealizedPnl.add(realizedPnl);

        return new PortfolioAssetPerformanceResponse(
            row.getSymbol(),
            row.getExchange(),
            currentBalance,
            totalInvested,
            avgBuyPrice,
            currentUnitPrice,
            currentValue,
            unrealizedPnl,
            unrealizedPnlPercent,
            realizedPnl,
            totalPnl
        );
    }

    private static BigDecimal nullSafe(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private static String key(String symbol, String exchange) {
        return symbol + "::" + exchange;
    }

    private static BigDecimal percentage(BigDecimal value, BigDecimal base) {
        if (base == null || base.compareTo(ZERO) == 0) {
            return ZERO;
        }
        return value.divide(base, 8, RoundingMode.HALF_UP).multiply(HUNDRED);
    }
}
