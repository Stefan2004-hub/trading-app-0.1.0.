package com.trading.domain.repository;

import com.trading.domain.entity.MarketAlert;
import com.trading.domain.enums.MarketAlertStrategyType;
import com.trading.domain.enums.MarketAlertType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MarketAlertRepository extends JpaRepository<MarketAlert, UUID> {

    List<MarketAlert> findAllByUser_IdOrderByTriggerDateDescCreatedAtDesc(UUID userId);

    Optional<MarketAlert> findByIdAndUser_Id(UUID id, UUID userId);

    boolean existsByUser_IdAndAsset_IdAndAlertTypeAndStrategyTypeAndTriggerDate(
        UUID userId,
        UUID assetId,
        MarketAlertType alertType,
        MarketAlertStrategyType strategyType,
        LocalDate triggerDate
    );

    long deleteAllByUser_Id(UUID userId);
}
