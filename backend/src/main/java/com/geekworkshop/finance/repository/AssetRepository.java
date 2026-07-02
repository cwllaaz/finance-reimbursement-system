package com.geekworkshop.finance.repository;

import com.geekworkshop.finance.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, Long> {
    @Query("""
        select a from Asset a
        left join fetch a.custodian
        left join fetch a.claimedBy
        join fetch a.acceptance ac
        left join fetch ac.acceptedBy
        join fetch ac.purchaseApplication p
        left join fetch p.department
        order by a.createdAt desc
        """)
    List<Asset> findAllDetails();

    @Query("""
        select a from Asset a
        left join fetch a.custodian
        left join fetch a.claimedBy
        join fetch a.acceptance ac
        left join fetch ac.acceptedBy
        join fetch ac.purchaseApplication p
        left join fetch p.department
        where a.id = :id
        """)
    Optional<Asset> findDetailById(@Param("id") Long id);

    Optional<Asset> findTopByAssetNumberStartingWithOrderByAssetNumberDesc(String prefix);
}
