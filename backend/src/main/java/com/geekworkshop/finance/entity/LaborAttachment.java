package com.geekworkshop.finance.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "labor_attachment")
public class LaborAttachment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "labor_application_id", nullable = false)
    private LaborApplication laborApplication;
    @Enumerated(EnumType.STRING)
    @Column(name = "attachment_type", nullable = false, length = 40)
    private LaborAttachmentType attachmentType;
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;
    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;
    @Column(name = "file_type", length = 120)
    private String fileType;
    @Column(name = "file_size")
    private Long fileSize;

    public Long getId() { return id; }
    public LaborApplication getLaborApplication() { return laborApplication; }
    public void setLaborApplication(LaborApplication laborApplication) { this.laborApplication = laborApplication; }
    public LaborAttachmentType getAttachmentType() { return attachmentType; }
    public void setAttachmentType(LaborAttachmentType attachmentType) { this.attachmentType = attachmentType; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
}
