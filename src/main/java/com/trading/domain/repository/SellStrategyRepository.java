package com.trading.domain.repository;

import com.trading.domain.entity.SellStrategy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SellStrategyRepository extends JpaRepository<SellStrategy, UUID> {

    List<SellStrategy> findAllByUser_IdOrderByUpdatedAtDesc(UUID userId);

    Optional<SellStrategy> findByUser_IdAndAsset_Id(UUID userId, UUID assetId);

    Optional<SellStrategy> findByIdAndUser_Id(UUID strategyId, UUID userId);
}
