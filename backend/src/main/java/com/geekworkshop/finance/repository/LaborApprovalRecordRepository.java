package com.geekworkshop.finance.repository;

import com.geekworkshop.finance.entity.LaborApprovalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface LaborApprovalRecordRepository extends JpaRepository<LaborApprovalRecord, Long> {
    @Query("""
        select r from LaborApprovalRecord r
        left join fetch r.approver
        where r.laborApplication.id = :id
        order by r.createdAt asc
        """)
    List<LaborApprovalRecord> findDetailsByApplicationId(@Param("id") Long id);
    @Query("""
        select r from LaborApprovalRecord r
        join fetch r.laborApplication a
        left join fetch a.applicant
        left join fetch a.department
        left join fetch r.approver
        where r.approver.id = :approverId
        order by r.createdAt desc
        """)
    List<LaborApprovalRecord> findDetailsByApproverId(@Param("approverId") Long approverId);
    void deleteByLaborApplicationId(Long id);
}
