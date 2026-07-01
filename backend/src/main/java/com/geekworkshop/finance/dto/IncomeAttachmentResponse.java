package com.geekworkshop.finance.dto;

import com.geekworkshop.finance.entity.IncomeAttachment;
import java.time.LocalDateTime;

public record IncomeAttachmentResponse(
        Long id, String fileName, String fileUrl, String fileType, Long fileSize, LocalDateTime createdAt
) {
    public static IncomeAttachmentResponse fromEntity(IncomeAttachment attachment) {
        return new IncomeAttachmentResponse(
                attachment.getId(),
                attachment.getFileName(),
                attachment.getFileUrl(),
                attachment.getFileType(),
                attachment.getFileSize(),
                attachment.getCreatedAt()
        );
    }
}
