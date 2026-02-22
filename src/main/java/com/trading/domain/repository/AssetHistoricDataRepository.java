package com.trading.domain.repository;

import com.trading.domain.entity.AssetHistoricData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface AssetHistoricDataRepository extends JpaRepository<AssetHistoricData, UUID> {

    void deleteByDayDate(LocalDate dayDate);

    @Query("select max(row.dayDate) from AssetHistoricData row where row.asset.id = :assetId")
    LocalDate findLatestDayDateByAssetId(@Param("assetId") UUID assetId);

    @Query(
        """
            select row.dayDate
            from AssetHistoricData row
            where row.asset.id = :assetId
              and row.dayDate between :startDate and :endDate
            """
    )
    Set<LocalDate> findExistingDayDates(
        @Param("assetId") UUID assetId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query(
        """
            select row
            from AssetHistoricData row
            where exists (
                select 1
                from Transaction tx
                where tx.user.id = :userId
                  and tx.asset.id = row.asset.id
            )
            order by row.dayDate desc, row.asset.symbol asc
            """
    )
    List<AssetHistoricData> findAllForUserInvestedAssets(@Param("userId") UUID userId);
}
