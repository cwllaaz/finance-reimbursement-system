package com.geekworkshop.finance.repository;

import com.geekworkshop.finance.entity.Reimbursement;
import com.geekworkshop.finance.entity.ReimbursementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReimbursementRepository extends JpaRepository<Reimbursement, Long> {

    @Query("""
            select r from Reimbursement r
            left join fetch r.applicant
            left join fetch r.department
            order by r.createdAt desc
            """)
    List<Reimbursement> findAllForExport();

    @Query("""
            select r from Reimbursement r
            left join fetch r.applicant
            left join fetch r.department
            where (:keyword is null
                or lower(r.title) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(r.description, '')) like lower(concat('%', :keyword, '%')))
              and (:status is null or r.status = :status)
            order by r.createdAt desc
            """)
    List<Reimbursement> search(
            @Param("keyword") String keyword,
            @Param("status") ReimbursementStatus status
    );

    @Query("""
            select r from Reimbursement r
            left join fetch r.applicant
            left join fetch r.department
            where r.id = :id
            """)
    Optional<Reimbursement> findDetailById(@Param("id") Long id);

    Optional<Reimbursement> findTopByApprovalNumberStartingWithOrderByApprovalNumberDesc(String prefix);
    Optional<Reimbursement> findByApprovalNumber(String approvalNumber);
}
