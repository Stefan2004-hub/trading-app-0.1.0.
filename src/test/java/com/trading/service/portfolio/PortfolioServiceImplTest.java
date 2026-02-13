package com.trading.service.portfolio;

import com.trading.domain.projection.UserAssetLatestPriceProjection;
import com.trading.domain.projection.UserAssetRealizedPnlProjection;
import com.trading.domain.projection.UserPortfolioPerformanceProjection;
import com.trading.domain.repository.TransactionRepository;
import com.trading.dto.portfolio.PortfolioAssetPerformanceResponse;
import com.trading.dto.portfolio.PortfolioSummaryResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private PortfolioServiceImpl portfolioService;

    @Test
    void getPerformanceCalculatesInvestedCurrentAndUnrealizedMetrics() {
        UUID userId = UUID.randomUUID();

        when(transactionRepository.findPortfolioPerformanceByUserId(userId)).thenReturn(
            List.of(
                new PortfolioProjectionRow("BTC", "Binance", "1.2", "60000", "50000")
            )
        );
        when(transactionRepository.findLatestUnitPricesByUserId(userId)).thenReturn(
            List.of(
                new LatestPriceRow("BTC", "Binance", "55000")
            )
        );
        when(transactionRepository.findRealizedPnlByUserId(userId)).thenReturn(
            List.of(
                new RealizedPnlRow("BTC", "Binance", "1500")
            )
        );

        List<PortfolioAssetPerformanceResponse> response = portfolioService.getPerformance(userId);
        PortfolioAssetPerformanceResponse row = response.get(0);

        assertEquals(1, response.size());
        assertEquals(0, row.currentBalance().compareTo(new BigDecimal("1.2")));
        assertEquals(0, row.totalInvestedUsd().compareTo(new BigDecimal("60000")));
        assertEquals(0, row.currentUnitPriceUsd().compareTo(new BigDecimal("55000")));
        assertEquals(0, row.currentValueUsd().compareTo(new BigDecimal("66000.0")));
        assertEquals(0, row.unrealizedPnlUsd().compareTo(new BigDecimal("6000.0")));
        assertEquals(0, row.unrealizedPnlPercent().compareTo(new BigDecimal("10.00000000")));
        assertEquals(0, row.realizedPnlUsd().compareTo(new BigDecimal("1500")));
        assertEquals(0, row.totalPnlUsd().compareTo(new BigDecimal("7500.0")));
    }

    @Test
    void getSummaryAggregatesPerformanceRows() {
        UUID userId = UUID.randomUUID();

        when(transactionRepository.findPortfolioPerformanceByUserId(userId)).thenReturn(
            List.of(
                new PortfolioProjectionRow("BTC", "Binance", "1.2", "60000", "50000"),
                new PortfolioProjectionRow("ETH", "Coinbase", "2", "4000", "2000")
            )
        );
        when(transactionRepository.findLatestUnitPricesByUserId(userId)).thenReturn(
            List.of(
                new LatestPriceRow("BTC", "Binance", "55000"),
                new LatestPriceRow("ETH", "Coinbase", "1800")
            )
        );
        when(transactionRepository.findRealizedPnlByUserId(userId)).thenReturn(
            List.of(
                new RealizedPnlRow("BTC", "Binance", "1500"),
                new RealizedPnlRow("ETH", "Coinbase", "-200")
            )
        );

        PortfolioSummaryResponse summary = portfolioService.getSummary(userId);

        assertEquals(0, summary.totalInvestedUsd().compareTo(new BigDecimal("64000")));
        assertEquals(0, summary.totalCurrentValueUsd().compareTo(new BigDecimal("69600.0")));
        assertEquals(0, summary.totalUnrealizedPnlUsd().compareTo(new BigDecimal("5600.0")));
        assertEquals(0, summary.totalUnrealizedPnlPercent().compareTo(new BigDecimal("8.75000000")));
        assertEquals(0, summary.totalRealizedPnlUsd().compareTo(new BigDecimal("1300")));
        assertEquals(0, summary.totalPnlUsd().compareTo(new BigDecimal("6900.0")));
        assertEquals(2, summary.assets().size());
    }

    private record PortfolioProjectionRow(
        String symbol,
        String exchange,
        BigDecimal currentBalance,
        BigDecimal totalInvestedUsd,
        BigDecimal avgBuyPrice
    ) implements UserPortfolioPerformanceProjection {
        private PortfolioProjectionRow(String symbol, String exchange, String currentBalance, String totalInvestedUsd, String avgBuyPrice) {
            this(symbol, exchange, new BigDecimal(currentBalance), new BigDecimal(totalInvestedUsd), new BigDecimal(avgBuyPrice));
        }

        @Override
        public UUID getUserId() {
            return null;
        }

        @Override
        public String getSymbol() {
            return symbol;
        }

        @Override
        public String getExchange() {
            return exchange;
        }

        @Override
        public BigDecimal getCurrentBalance() {
            return currentBalance;
        }

        @Override
        public BigDecimal getTotalInvestedUsd() {
            return totalInvestedUsd;
        }

        @Override
        public BigDecimal getAvgBuyPrice() {
            return avgBuyPrice;
        }
    }

    private record LatestPriceRow(
        String symbol,
        String exchange,
        BigDecimal latestUnitPriceUsd
    ) implements UserAssetLatestPriceProjection {
        private LatestPriceRow(String symbol, String exchange, String latestUnitPriceUsd) {
            this(symbol, exchange, new BigDecimal(latestUnitPriceUsd));
        }

        @Override
        public String getSymbol() {
            return symbol;
        }

        @Override
        public String getExchange() {
            return exchange;
        }

        @Override
        public BigDecimal getLatestUnitPriceUsd() {
            return latestUnitPriceUsd;
        }
    }

    private record RealizedPnlRow(
        String symbol,
        String exchange,
        BigDecimal realizedPnl
    ) implements UserAssetRealizedPnlProjection {
        private RealizedPnlRow(String symbol, String exchange, String realizedPnl) {
            this(symbol, exchange, new BigDecimal(realizedPnl));
        }

        @Override
        public String getSymbol() {
            return symbol;
        }

        @Override
        public String getExchange() {
            return exchange;
        }

        @Override
        public BigDecimal getRealizedPnl() {
            return realizedPnl;
        }
    }
}
