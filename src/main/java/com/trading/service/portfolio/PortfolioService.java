package com.trading.service.portfolio;

import com.trading.dto.portfolio.PortfolioAssetPerformanceResponse;
import com.trading.dto.portfolio.PortfolioSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface PortfolioService {

    PortfolioSummaryResponse getSummary(UUID userId);

    List<PortfolioAssetPerformanceResponse> getPerformance(UUID userId);
}
