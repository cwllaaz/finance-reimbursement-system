package com.geekworkshop.finance.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public class AssetAcceptanceRequest {
    @NotNull
    private Long purchaseApplicationId;
    @NotNull
    private LocalDateTime receivedAt;
    @NotBlank @Size(max = 200)
    private String storageLocation;
    @Size(max = 500)
    private String remark;

    public Long getPurchaseApplicationId() { return purchaseApplicationId; }
    public void setPurchaseApplicationId(Long purchaseApplicationId) { this.purchaseApplicationId = purchaseApplicationId; }
    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }
    public String getStorageLocation() { return storageLocation; }
    public void setStorageLocation(String storageLocation) { this.storageLocation = storageLocation; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
