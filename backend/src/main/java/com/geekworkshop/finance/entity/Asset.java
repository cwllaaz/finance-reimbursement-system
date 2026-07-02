package com.geekworkshop.finance.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "asset")
public class Asset extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "asset_number", nullable = false, unique = true, length = 30)
    private String assetNumber;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acceptance_id", nullable = false)
    private AssetAcceptance acceptance;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_item_id", nullable = false)
    private PurchaseItem purchaseItem;
    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;
    @Column(length = 200)
    private String specification;
    @Column(length = 200)
    private String manufacturer;
    @Column(nullable = false)
    private Integer quantity;
    @Column(name = "total_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalPrice;
    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;
    @Column(nullable = false, length = 200)
    private String location;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AssetStatus status = AssetStatus.IN_STOCK;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custodian_id")
    private AppUser custodian;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claimed_by")
    private AppUser claimedBy;
    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;

    public Long getId() { return id; }
    public String getAssetNumber() { return assetNumber; }
    public void setAssetNumber(String assetNumber) { this.assetNumber = assetNumber; }
    public AssetAcceptance getAcceptance() { return acceptance; }
    public void setAcceptance(AssetAcceptance acceptance) { this.acceptance = acceptance; }
    public PurchaseItem getPurchaseItem() { return purchaseItem; }
    public void setPurchaseItem(PurchaseItem purchaseItem) { this.purchaseItem = purchaseItem; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getSpecification() { return specification; }
    public void setSpecification(String specification) { this.specification = specification; }
    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public AssetStatus getStatus() { return status; }
    public void setStatus(AssetStatus status) { this.status = status; }
    public AppUser getCustodian() { return custodian; }
    public void setCustodian(AppUser custodian) { this.custodian = custodian; }
    public AppUser getClaimedBy() { return claimedBy; }
    public void setClaimedBy(AppUser claimedBy) { this.claimedBy = claimedBy; }
    public LocalDateTime getClaimedAt() { return claimedAt; }
    public void setClaimedAt(LocalDateTime claimedAt) { this.claimedAt = claimedAt; }
}
