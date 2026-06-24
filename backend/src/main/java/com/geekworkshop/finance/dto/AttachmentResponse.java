package com.geekworkshop.finance.dto;

import com.geekworkshop.finance.entity.Attachment;

import java.time.LocalDateTime;

public class AttachmentResponse {

    private Long id;
    private Long reimbursementId;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private Long fileSize;
    private LocalDateTime createdAt;

    public static AttachmentResponse fromEntity(Attachment attachment) {
        AttachmentResponse response = new AttachmentResponse();
        response.id = attachment.getId();
        response.reimbursementId = attachment.getReimbursement().getId();
        response.fileName = attachment.getFileName();
        response.fileUrl = attachment.getFileUrl();
        response.fileType = attachment.getFileType();
        response.fileSize = attachment.getFileSize();
        response.createdAt = attachment.getCreatedAt();
        return response;
    }

    public Long getId() {
        return id;
    }

    public Long getReimbursementId() {
        return reimbursementId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public String getFileType() {
        return fileType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
