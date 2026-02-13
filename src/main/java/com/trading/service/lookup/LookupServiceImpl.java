package com.trading.service.lookup;

import com.trading.domain.entity.Asset;
import com.trading.domain.entity.Exchange;
import com.trading.domain.repository.AssetRepository;
import com.trading.domain.repository.ExchangeRepository;
import com.trading.dto.lookup.AssetLookupResponse;
import com.trading.dto.lookup.ExchangeLookupResponse;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.List;

@Service
@ConditionalOnBean({AssetRepository.class, ExchangeRepository.class, EntityManagerFactory.class, DataSource.class})
public class LookupServiceImpl implements LookupService {

    private final AssetRepository assetRepository;
    private final ExchangeRepository exchangeRepository;

    public LookupServiceImpl(AssetRepository assetRepository, ExchangeRepository exchangeRepository) {
        this.assetRepository = assetRepository;
        this.exchangeRepository = exchangeRepository;
    }

    @Override
    public List<AssetLookupResponse> listAssets() {
        return assetRepository.findAllByOrderBySymbolAsc().stream()
            .map(LookupServiceImpl::toAssetResponse)
            .toList();
    }

    @Override
    public List<ExchangeLookupResponse> listExchanges() {
        return exchangeRepository.findAllByOrderByNameAsc().stream()
            .map(LookupServiceImpl::toExchangeResponse)
            .toList();
    }

    private static AssetLookupResponse toAssetResponse(Asset asset) {
        return new AssetLookupResponse(asset.getId(), asset.getSymbol(), asset.getName());
    }

    private static ExchangeLookupResponse toExchangeResponse(Exchange exchange) {
        return new ExchangeLookupResponse(exchange.getId(), exchange.getName());
    }
}
