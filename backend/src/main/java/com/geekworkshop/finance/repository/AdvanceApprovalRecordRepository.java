package com.geekworkshop.finance.repository;

import com.geekworkshop.finance.entity.AdvanceApprovalRecord;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface AdvanceApprovalRecordRepository extends JpaRepository<AdvanceApprovalRecord, Long> {
    @Query("""
        select r from AdvanceApprovalRecord r left join fetch r.approver
        where r.advanceApplication.id = :id order by r.createdAt asc
        """)
    List<AdvanceApprovalRecord> findDetailsByApplicationId(@Param("id") Long id);
    @Query("""
        select r from AdvanceApprovalRecord r
        join fetch r.advanceApplication a
        left join fetch a.applicant
        left join fetch a.department
        left join fetch r.approver
        where r.approver.id = :approverId
        order by r.createdAt desc
        """)
    List<AdvanceApprovalRecord> findDetailsByApproverId(@Param("approverId") Long approverId);
    void deleteByAdvanceApplicationId(Long id);
}
