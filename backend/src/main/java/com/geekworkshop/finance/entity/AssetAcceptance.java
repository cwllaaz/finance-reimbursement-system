package com.geekworkshop.finance.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "asset_acceptance")
public class AssetAcceptance extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "acceptance_number", nullable = false, unique = true, length = 30)
    private String acceptanceNumber;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_application_id", nullable = false, unique = true)
    private PurchaseApplication purchaseApplication;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_by")
    private AppUser acceptedBy;
    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;
    @Column(name = "storage_location", nullable = false, length = 200)
    private String storageLocation;
    @Column(length = 500)
    private String remark;

    public Long getId() { return id; }
    public String getAcceptanceNumber() { return acceptanceNumber; }
    public void setAcceptanceNumber(String acceptanceNumber) { this.acceptanceNumber = acceptanceNumber; }
    public PurchaseApplication getPurchaseApplication() { return purchaseApplication; }
    public void setPurchaseApplication(PurchaseApplication purchaseApplication) { this.purchaseApplication = purchaseApplication; }
    public AppUser getAcceptedBy() { return acceptedBy; }
    public void setAcceptedBy(AppUser acceptedBy) { this.acceptedBy = acceptedBy; }
    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }
    public String getStorageLocation() { return storageLocation; }
    public void setStorageLocation(String storageLocation) { this.storageLocation = storageLocation; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
