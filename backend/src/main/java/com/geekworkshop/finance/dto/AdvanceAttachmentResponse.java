package com.geekworkshop.finance.dto;

import com.geekworkshop.finance.entity.*;
import java.time.LocalDateTime;

public record AdvanceAttachmentResponse(
        Long id, AdvanceAttachmentType attachmentType, String fileName, String fileUrl,
        String fileType, Long fileSize, LocalDateTime createdAt
) {
    public static AdvanceAttachmentResponse from(AdvanceAttachment value) {
        return new AdvanceAttachmentResponse(value.getId(), value.getAttachmentType(), value.getFileName(),
                value.getFileUrl(), value.getFileType(), value.getFileSize(), value.getCreatedAt());
    }
}
