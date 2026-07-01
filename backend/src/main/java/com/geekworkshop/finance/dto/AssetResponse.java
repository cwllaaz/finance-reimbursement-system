package com.geekworkshop.finance.dto;

import com.geekworkshop.finance.entity.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AssetResponse(
        Long id, String assetNumber, String acceptanceNumber,
        Long purchaseApplicationId, String purchaseApplicationNumber,
        String itemName, String specification, String manufacturer,
        Integer quantity, BigDecimal totalPrice, LocalDateTime receivedAt,
        String location, AssetStatus status, Long custodianId, String custodianName,
        String departmentName, List<AssetHistoryResponse> history
) {
    public static AssetResponse fromEntity(Asset asset, List<AssetHistoryResponse> history) {
        PurchaseApplication purchase = asset.getAcceptance().getPurchaseApplication();
        return new AssetResponse(
                asset.getId(), asset.getAssetNumber(), asset.getAcceptance().getAcceptanceNumber(),
                purchase.getId(), purchase.getApplicationNumber(), asset.getItemName(),
                asset.getSpecification(), asset.getManufacturer(), asset.getQuantity(),
                asset.getTotalPrice(), asset.getReceivedAt(), asset.getLocation(), asset.getStatus(),
                asset.getCustodian() == null ? null : asset.getCustodian().getId(),
                asset.getCustodian() == null ? null : asset.getCustodian().getRealName(),
                purchase.getDepartment() == null ? null : purchase.getDepartment().getName(), history);
    }
}
