package com.geekworkshop.finance.dto;

import com.geekworkshop.finance.entity.ApprovalAction;
import com.geekworkshop.finance.entity.ApprovalRecord;

import java.time.LocalDateTime;

public class ApprovalRecordResponse {

    private Long id;
    private Long reimbursementId;
    private String approverName;
    private String approvalNode;
    private ApprovalAction action;
    private String comment;
    private LocalDateTime createdAt;

    public static ApprovalRecordResponse fromEntity(ApprovalRecord record) {
        ApprovalRecordResponse response = new ApprovalRecordResponse();
        response.id = record.getId();
        response.reimbursementId = record.getReimbursement().getId();
        response.approverName = record.getApprover() == null ? null : record.getApprover().getRealName();
        response.approvalNode = record.getApprovalNode();
        response.action = record.getAction();
        response.comment = record.getComment();
        response.createdAt = record.getCreatedAt();
        return response;
    }

    public Long getId() {
        return id;
    }

    public Long getReimbursementId() {
        return reimbursementId;
    }

    public String getApproverName() {
        return approverName;
    }

    public String getApprovalNode() {
        return approvalNode;
    }

    public ApprovalAction getAction() {
        return action;
    }

    public String getComment() {
        return comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
