package com.geekworkshop.finance.dto;

import com.geekworkshop.finance.entity.*;
import java.time.LocalDateTime;

public record AssetHistoryResponse(
        Long id, String receiptNumber, AssetHistoryAction action,
        Long operatorId, String operatorName, Long actualUserId, String actualUserName,
        String custodianName, String location,
        AssetStatus assetStatus, String remark, LocalDateTime createdAt
) {
    public static AssetHistoryResponse fromEntity(AssetHistory history) {
        return new AssetHistoryResponse(
                history.getId(), history.getReceiptNumber(), history.getAction(),
                history.getOperator().getId(),
                history.getOperator().getRealName(),
                history.getCustodian() == null ? null : history.getCustodian().getId(),
                history.getCustodian() == null ? null : history.getCustodian().getRealName(),
                history.getCustodian() == null ? null : history.getCustodian().getRealName(),
                history.getLocation(), history.getAssetStatus(), history.getRemark(), history.getCreatedAt());
    }
}
