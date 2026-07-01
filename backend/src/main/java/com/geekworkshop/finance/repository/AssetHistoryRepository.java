package com.geekworkshop.finance.repository;

import com.geekworkshop.finance.entity.AssetHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AssetHistoryRepository extends JpaRepository<AssetHistory, Long> {
    List<AssetHistory> findByAssetIdOrderByCreatedAtAsc(Long assetId);
    Optional<AssetHistory> findTopByReceiptNumberStartingWithOrderByReceiptNumberDesc(String prefix);
}
