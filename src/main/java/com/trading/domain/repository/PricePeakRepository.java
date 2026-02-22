package com.trading.domain.repository;

import com.trading.domain.entity.PricePeak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PricePeakRepository extends JpaRepository<PricePeak, UUID> {

    List<PricePeak> findAllByUser_IdOrderByUpdatedAtDesc(UUID userId);

    @Query(
        """
            SELECT pp
            FROM PricePeak pp
            JOIN pp.asset a
            WHERE pp.user.id = :userId
              AND (
                :searchPattern IS NULL
                OR LOWER(a.symbol) LIKE LOWER(:searchPattern)
                OR LOWER(a.name) LIKE LOWER(:searchPattern)
              )
            ORDER BY pp.updatedAt DESC
            """
    )
    List<PricePeak> findByUser_IdAndSearch(
        @Param("userId") UUID userId,
        @Param("searchPattern") String searchPattern
    );

    Optional<PricePeak> findByUser_IdAndAsset_Id(UUID userId, UUID assetId);

    Optional<PricePeak> findByUser_IdAndAsset_IdAndActiveTrue(UUID userId, UUID assetId);

    Optional<PricePeak> findByIdAndUser_Id(UUID pricePeakId, UUID userId);

    List<PricePeak> findAllByUser_IdAndLastBuyTransaction_IdIn(UUID userId, Collection<UUID> transactionIds);
}
