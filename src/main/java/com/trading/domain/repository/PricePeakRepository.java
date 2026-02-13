package com.trading.domain.repository;

import com.trading.domain.entity.PricePeak;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PricePeakRepository extends JpaRepository<PricePeak, UUID> {

    List<PricePeak> findAllByUser_IdOrderByUpdatedAtDesc(UUID userId);

    Optional<PricePeak> findByUser_IdAndAsset_Id(UUID userId, UUID assetId);

    Optional<PricePeak> findByUser_IdAndAsset_IdAndActiveTrue(UUID userId, UUID assetId);

    Optional<PricePeak> findByIdAndUser_Id(UUID pricePeakId, UUID userId);
}
