package com.geekworkshop.finance.dto;

import com.geekworkshop.finance.entity.ApprovalAction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ApprovalRequest {

    @NotNull(message = "action is required")
    private ApprovalAction action;

    @Size(max = 500, message = "comment is too long")
    private String comment;

    public ApprovalAction getAction() {
        return action;
    }

    public void setAction(ApprovalAction action) {
        this.action = action;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
