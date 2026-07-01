package com.geekworkshop.finance.dto;

import com.geekworkshop.finance.entity.ApprovalAction;
import com.geekworkshop.finance.entity.PurchaseApprovalRecord;
import java.time.LocalDateTime;

public record PurchaseApprovalResponse(
        Long id, String approvalNode, ApprovalAction action, String comment,
        Long approverId, String approverName, String approverRole, LocalDateTime createdAt
) {
    public static PurchaseApprovalResponse fromEntity(PurchaseApprovalRecord record) {
        return new PurchaseApprovalResponse(record.getId(), record.getApprovalNode(), record.getAction(),
                record.getComment(), record.getApprover() == null ? null : record.getApprover().getId(),
                record.getApprover() == null ? null : record.getApprover().getRealName(),
                record.getApprover() == null ? null : record.getApprover().getRole().name(),
                record.getCreatedAt());
    }
}
