package com.trading.domain.repository;

import com.trading.domain.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssetRepository extends JpaRepository<Asset, UUID> {

    Optional<Asset> findBySymbolIgnoreCase(String symbol);

    Optional<Asset> findByCoinGeckoIdIgnoreCase(String coinGeckoId);

    List<Asset> findAllByOrderBySymbolAsc();

    List<Asset> findBySymbolContainingIgnoreCaseOrNameContainingIgnoreCaseOrderBySymbolAsc(String symbol, String name);

    @Query(
        """
            select asset
            from Asset asset
            where not exists (
                select 1
                from AssetHistoricData row
                where row.asset.id = asset.id
                  and row.dayDate = :dayDate
            )
            order by asset.symbol asc
            """
    )
    List<Asset> findAssetsMissingHistoricalDataForDay(@Param("dayDate") LocalDate dayDate);
}
