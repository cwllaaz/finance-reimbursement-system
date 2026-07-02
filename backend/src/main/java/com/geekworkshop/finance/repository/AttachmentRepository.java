package com.geekworkshop.finance.repository;

import com.geekworkshop.finance.entity.Attachment;
import com.geekworkshop.finance.entity.AttachmentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByReimbursementIdOrderByCreatedAtDesc(Long reimbursementId);

    List<Attachment> findByReimbursementIdAndAttachmentTypeOrderByCreatedAtDesc(
            Long reimbursementId,
            AttachmentType attachmentType
    );

    boolean existsByReimbursementIdAndAttachmentType(Long reimbursementId, AttachmentType attachmentType);

    boolean existsByReimbursementIdAndAttachmentTypeIn(
            Long reimbursementId,
            List<AttachmentType> attachmentTypes
    );
}
