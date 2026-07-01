package com.geekworkshop.finance.repository;

import com.geekworkshop.finance.entity.LaborAttachment;
import com.geekworkshop.finance.entity.LaborAttachmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LaborAttachmentRepository extends JpaRepository<LaborAttachment, Long> {
    List<LaborAttachment> findByLaborApplicationIdOrderByCreatedAtAsc(Long id);
    boolean existsByLaborApplicationIdAndAttachmentType(Long id, LaborAttachmentType type);
    void deleteByLaborApplicationId(Long id);
}
