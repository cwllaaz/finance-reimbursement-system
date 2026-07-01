package com.geekworkshop.finance.repository;

import com.geekworkshop.finance.entity.PurchaseAttachment;
import com.geekworkshop.finance.entity.PurchaseAttachmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PurchaseAttachmentRepository extends JpaRepository<PurchaseAttachment, Long> {
    List<PurchaseAttachment> findByPurchaseApplicationIdOrderByCreatedAtAsc(Long purchaseApplicationId);
    boolean existsByPurchaseApplicationIdAndAttachmentType(Long purchaseApplicationId, PurchaseAttachmentType type);
    void deleteByPurchaseApplicationId(Long purchaseApplicationId);
}
