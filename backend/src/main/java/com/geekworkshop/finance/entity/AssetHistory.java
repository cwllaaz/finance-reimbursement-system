package com.geekworkshop.finance.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "asset_history")
public class AssetHistory extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;
    @Column(name = "receipt_number", length = 30)
    private String receiptNumber;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AssetHistoryAction action;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id", nullable = false)
    private AppUser operator;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custodian_id")
    private AppUser custodian;
    @Column(length = 200)
    private String location;
    @Enumerated(EnumType.STRING)
    @Column(name = "asset_status", nullable = false, length = 40)
    private AssetStatus assetStatus;
    @Column(length = 500)
    private String remark;

    public Long getId() { return id; }
    public Asset getAsset() { return asset; }
    public void setAsset(Asset asset) { this.asset = asset; }
    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }
    public AssetHistoryAction getAction() { return action; }
    public void setAction(AssetHistoryAction action) { this.action = action; }
    public AppUser getOperator() { return operator; }
    public void setOperator(AppUser operator) { this.operator = operator; }
    public AppUser getCustodian() { return custodian; }
    public void setCustodian(AppUser custodian) { this.custodian = custodian; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public AssetStatus getAssetStatus() { return assetStatus; }
    public void setAssetStatus(AssetStatus assetStatus) { this.assetStatus = assetStatus; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
