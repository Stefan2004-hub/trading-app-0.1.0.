package com.trading.domain.repository;

import com.trading.domain.entity.StrategyAlert;
import com.trading.domain.enums.StrategyAlertStatus;
import com.trading.domain.enums.StrategyType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StrategyAlertRepository extends JpaRepository<StrategyAlert, UUID> {

    List<StrategyAlert> findAllByUser_IdOrderByCreatedAtDesc(UUID userId);

    List<StrategyAlert> findAllByUser_IdAndStatusOrderByCreatedAtDesc(UUID userId, StrategyAlertStatus status);

    Optional<StrategyAlert> findByIdAndUser_Id(UUID alertId, UUID userId);

    boolean existsByUser_IdAndAsset_IdAndStrategyTypeAndStatus(
        UUID userId,
        UUID assetId,
        StrategyType strategyType,
        StrategyAlertStatus status
    );
}
