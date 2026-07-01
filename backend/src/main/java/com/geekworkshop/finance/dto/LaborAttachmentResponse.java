package com.geekworkshop.finance.dto;

import com.geekworkshop.finance.entity.LaborAttachment;
import com.geekworkshop.finance.entity.LaborAttachmentType;
import java.time.LocalDateTime;

public record LaborAttachmentResponse(
        Long id, LaborAttachmentType attachmentType, String fileName,
        String fileUrl, String fileType, Long fileSize, LocalDateTime createdAt
) {
    public static LaborAttachmentResponse fromEntity(LaborAttachment value) {
        return new LaborAttachmentResponse(value.getId(), value.getAttachmentType(), value.getFileName(),
                value.getFileUrl(), value.getFileType(), value.getFileSize(), value.getCreatedAt());
    }
}
