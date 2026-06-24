package com.geekworkshop.finance.repository;

import com.geekworkshop.finance.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByReimbursementIdOrderByCreatedAtDesc(Long reimbursementId);
}
