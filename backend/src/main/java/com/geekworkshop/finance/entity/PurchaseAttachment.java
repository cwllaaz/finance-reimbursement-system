package com.geekworkshop.finance.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "purchase_attachment")
public class PurchaseAttachment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_application_id", nullable = false)
    private PurchaseApplication purchaseApplication;
    @Column(name = "file_name", nullable = false, length = 200)
    private String fileName;
    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;
    @Column(name = "file_type", length = 100)
    private String fileType;
    @Column(name = "file_size")
    private Long fileSize;
    @Enumerated(EnumType.STRING)
    @Column(name = "attachment_type", nullable = false, length = 40)
    private PurchaseAttachmentType attachmentType;

    public Long getId() { return id; }
    public PurchaseApplication getPurchaseApplication() { return purchaseApplication; }
    public void setPurchaseApplication(PurchaseApplication purchaseApplication) { this.purchaseApplication = purchaseApplication; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public PurchaseAttachmentType getAttachmentType() { return attachmentType; }
    public void setAttachmentType(PurchaseAttachmentType attachmentType) { this.attachmentType = attachmentType; }
}
