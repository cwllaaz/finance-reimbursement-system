package com.geekworkshop.finance.repository;

import com.geekworkshop.finance.entity.ApprovalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ApprovalRecordRepository extends JpaRepository<ApprovalRecord, Long> {

    @Query("""
            select a from ApprovalRecord a
            left join fetch a.reimbursement
            left join fetch a.approver
            where a.reimbursement.id = :reimbursementId
            order by a.createdAt asc
            """)
    List<ApprovalRecord> findDetailByReimbursementIdOrderByCreatedAtAsc(@Param("reimbursementId") Long reimbursementId);

    List<ApprovalRecord> findByReimbursementIdOrderByCreatedAtAsc(Long reimbursementId);
}
