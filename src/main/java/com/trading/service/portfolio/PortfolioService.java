package com.trading.service.portfolio;

import com.trading.dto.portfolio.PortfolioAssetPerformanceResponse;
import com.trading.dto.portfolio.PortfolioSummaryResponse;
import com.trading.dto.portfolio.AssetSummaryDTO;

import java.util.List;
import java.util.UUID;

public interface PortfolioService {

    PortfolioSummaryResponse getSummary(UUID userId);

    List<PortfolioAssetPerformanceResponse> getPerformance(UUID userId);

    List<AssetSummaryDTO> getAssetSummary(UUID userId);
}
