package com.geekworkshop.finance.dto;

import com.geekworkshop.finance.entity.AssetClaimApplication;
import com.geekworkshop.finance.entity.AssetClaimStatus;
import java.time.LocalDateTime;

public record AssetClaimApplicationResponse(
        Long id, Long assetId, String assetNumber, String itemName,
        Long applicantId, String applicantName, String departmentName,
        String useLocation, String reason, AssetClaimStatus status,
        String reviewedByName, String reviewComment, LocalDateTime reviewedAt,
        LocalDateTime createdAt
) {
    public static AssetClaimApplicationResponse fromEntity(AssetClaimApplication value) {
        return new AssetClaimApplicationResponse(
                value.getId(), value.getAsset().getId(), value.getAsset().getAssetNumber(),
                value.getAsset().getItemName(), value.getApplicant().getId(), value.getApplicant().getRealName(),
                value.getDepartment() == null ? null : value.getDepartment().getName(),
                value.getUseLocation(), value.getReason(), value.getStatus(),
                value.getReviewedBy() == null ? null : value.getReviewedBy().getRealName(),
                value.getReviewComment(), value.getReviewedAt(), value.getCreatedAt());
    }
}
