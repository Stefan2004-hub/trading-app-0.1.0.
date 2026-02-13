package com.trading.domain.repository;

import com.trading.domain.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AssetRepository extends JpaRepository<Asset, UUID> {

    Optional<Asset> findBySymbolIgnoreCase(String symbol);
}
