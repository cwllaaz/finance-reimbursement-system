package com.geekworkshop.finance.dto;

import com.geekworkshop.finance.entity.PurchaseApplication;
import com.geekworkshop.finance.entity.PurchaseStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PurchaseApplicationResponse(
        Long id, String applicationNumber, Long applicantId, String applicantName,
        Long departmentId, String departmentName, String applicantPhone, BigDecimal amount,
        String budgetNumber, String purchaseMethod, Boolean taxExempt, String useLocation,
        String purchaseReason, String assetAcceptanceNumber, PurchaseStatus status,
        LocalDateTime submittedAt, LocalDateTime createdAt, LocalDateTime updatedAt,
        List<PurchaseItemResponse> items, List<PurchaseAttachmentResponse> attachments,
        List<PurchaseApprovalResponse> approvalRecords
) {
    public static PurchaseApplicationResponse fromEntity(
            PurchaseApplication application,
            List<PurchaseAttachmentResponse> attachments,
            List<PurchaseApprovalResponse> approvalRecords
    ) {
        return new PurchaseApplicationResponse(
                application.getId(), application.getApplicationNumber(),
                application.getApplicant().getId(), application.getApplicant().getRealName(),
                application.getDepartment() == null ? null : application.getDepartment().getId(),
                application.getDepartment() == null ? null : application.getDepartment().getName(),
                application.getApplicantPhone(), application.getAmount(), application.getBudgetNumber(),
                application.getPurchaseMethod(), application.getTaxExempt(), application.getUseLocation(),
                application.getPurchaseReason(), application.getAssetAcceptanceNumber(), application.getStatus(),
                application.getSubmittedAt(), application.getCreatedAt(), application.getUpdatedAt(),
                application.getItems().stream().map(PurchaseItemResponse::fromEntity).toList(),
                attachments, approvalRecords);
    }
}
