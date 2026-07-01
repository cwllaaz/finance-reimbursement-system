package com.geekworkshop.finance.repository;

import com.geekworkshop.finance.entity.PurchaseApprovalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PurchaseApprovalRecordRepository extends JpaRepository<PurchaseApprovalRecord, Long> {
    List<PurchaseApprovalRecord> findByPurchaseApplicationIdOrderByCreatedAtAsc(Long purchaseApplicationId);
    @Query("""
        select r from PurchaseApprovalRecord r
        join fetch r.purchaseApplication a
        left join fetch a.applicant
        left join fetch a.department
        left join fetch r.approver
        where r.approver.id = :approverId
        order by r.createdAt desc
        """)
    List<PurchaseApprovalRecord> findDetailsByApproverId(@Param("approverId") Long approverId);
    void deleteByPurchaseApplicationId(Long purchaseApplicationId);
}
