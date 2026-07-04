package com.geekworkshop.finance.repository;

import com.geekworkshop.finance.entity.AssetClaimApplication;
import com.geekworkshop.finance.entity.AssetClaimStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface AssetClaimApplicationRepository extends JpaRepository<AssetClaimApplication, Long> {
    boolean existsByAssetIdAndStatus(Long assetId, AssetClaimStatus status);

    @Query("""
        select r from AssetClaimApplication r
        join fetch r.asset
        join fetch r.applicant
        left join fetch r.department
        left join fetch r.reviewedBy
        order by r.createdAt desc
        """)
    List<AssetClaimApplication> findAllDetails();

    @Query("""
        select r from AssetClaimApplication r
        join fetch r.asset
        join fetch r.applicant
        left join fetch r.department
        left join fetch r.reviewedBy
        where r.id = :id
        """)
    Optional<AssetClaimApplication> findDetailById(Long id);
}
