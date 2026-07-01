package com.geekworkshop.finance.repository;

import com.geekworkshop.finance.entity.IncomeAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncomeAttachmentRepository extends JpaRepository<IncomeAttachment, Long> {
    List<IncomeAttachment> findByIncomeRecordIdOrderByCreatedAtAsc(Long incomeRecordId);
    void deleteByIncomeRecordId(Long incomeRecordId);
}
