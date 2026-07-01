package com.geekworkshop.finance.dto;

import com.geekworkshop.finance.entity.PurchaseAttachment;
import com.geekworkshop.finance.entity.PurchaseAttachmentType;
import java.time.LocalDateTime;

public record PurchaseAttachmentResponse(
        Long id, String fileName, String fileUrl, String fileType, Long fileSize,
        PurchaseAttachmentType attachmentType, LocalDateTime createdAt
) {
    public static PurchaseAttachmentResponse fromEntity(PurchaseAttachment attachment) {
        return new PurchaseAttachmentResponse(attachment.getId(), attachment.getFileName(),
                attachment.getFileUrl(), attachment.getFileType(), attachment.getFileSize(),
                attachment.getAttachmentType(), attachment.getCreatedAt());
    }
}
