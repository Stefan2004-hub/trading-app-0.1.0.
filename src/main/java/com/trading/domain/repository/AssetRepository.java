package com.trading.domain.repository;

import com.trading.domain.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssetRepository extends JpaRepository<Asset, UUID> {

    Optional<Asset> findBySymbolIgnoreCase(String symbol);

    List<Asset> findAllByOrderBySymbolAsc();

    List<Asset> findBySymbolContainingIgnoreCaseOrNameContainingIgnoreCaseOrderBySymbolAsc(String symbol, String name);
}
