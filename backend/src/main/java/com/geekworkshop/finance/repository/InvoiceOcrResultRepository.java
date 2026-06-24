package com.geekworkshop.finance.repository;

import com.geekworkshop.finance.entity.InvoiceOcrResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InvoiceOcrResultRepository extends JpaRepository<InvoiceOcrResult, Long> {

    @Query("""
            select o from InvoiceOcrResult o
            left join fetch o.reimbursement
            left join fetch o.attachment
            where o.reimbursement.id = :reimbursementId
            """)
    Optional<InvoiceOcrResult> findByReimbursementId(@Param("reimbursementId") Long reimbursementId);
}
