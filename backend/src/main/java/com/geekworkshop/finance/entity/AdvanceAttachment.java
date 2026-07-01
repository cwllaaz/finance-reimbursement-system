package com.geekworkshop.finance.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "advance_attachment")
public class AdvanceAttachment extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "advance_application_id", nullable = false)
    private AdvanceApplication advanceApplication;
    @Enumerated(EnumType.STRING) @Column(name = "attachment_type", nullable = false, length = 40)
    private AdvanceAttachmentType attachmentType;
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;
    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;
    @Column(name = "file_type", length = 120)
    private String fileType;
    @Column(name = "file_size")
    private Long fileSize;
    public Long getId() { return id; }
    public AdvanceApplication getAdvanceApplication() { return advanceApplication; }
    public void setAdvanceApplication(AdvanceApplication value) { advanceApplication = value; }
    public AdvanceAttachmentType getAttachmentType() { return attachmentType; }
    public void setAttachmentType(AdvanceAttachmentType value) { attachmentType = value; }
    public String getFileName() { return fileName; }
    public void setFileName(String value) { fileName = value; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String value) { fileUrl = value; }
    public String getFileType() { return fileType; }
    public void setFileType(String value) { fileType = value; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long value) { fileSize = value; }
}
