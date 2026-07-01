package com.geekworkshop.finance.repository;

import com.geekworkshop.finance.entity.AssetAcceptance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AssetAcceptanceRepository extends JpaRepository<AssetAcceptance, Long> {
    boolean existsByPurchaseApplicationId(Long purchaseApplicationId);
    Optional<AssetAcceptance> findTopByAcceptanceNumberStartingWithOrderByAcceptanceNumberDesc(String prefix);
}
