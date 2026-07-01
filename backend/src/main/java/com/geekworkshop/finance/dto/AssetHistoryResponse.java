package com.geekworkshop.finance.dto;

import com.geekworkshop.finance.entity.*;
import java.time.LocalDateTime;

public record AssetHistoryResponse(
        Long id, String receiptNumber, AssetHistoryAction action,
        String operatorName, String custodianName, String location,
        AssetStatus assetStatus, String remark, LocalDateTime createdAt
) {
    public static AssetHistoryResponse fromEntity(AssetHistory history) {
        return new AssetHistoryResponse(
                history.getId(), history.getReceiptNumber(), history.getAction(),
                history.getOperator().getRealName(),
                history.getCustodian() == null ? null : history.getCustodian().getRealName(),
                history.getLocation(), history.getAssetStatus(), history.getRemark(), history.getCreatedAt());
    }
}
