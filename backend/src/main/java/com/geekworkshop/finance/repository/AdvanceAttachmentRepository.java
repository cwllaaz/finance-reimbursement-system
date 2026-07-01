package com.geekworkshop.finance.repository;

import com.geekworkshop.finance.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AdvanceAttachmentRepository extends JpaRepository<AdvanceAttachment, Long> {
    List<AdvanceAttachment> findByAdvanceApplicationIdOrderByCreatedAtAsc(Long id);
    boolean existsByAdvanceApplicationIdAndAttachmentType(Long id, AdvanceAttachmentType type);
    void deleteByAdvanceApplicationId(Long id);
}
