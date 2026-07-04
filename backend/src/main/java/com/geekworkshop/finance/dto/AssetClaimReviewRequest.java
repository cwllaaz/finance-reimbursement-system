package com.geekworkshop.finance.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AssetClaimReviewRequest {
    @NotNull private Boolean approved;
    @Size(max = 500) private String comment;
    public Boolean getApproved() { return approved; }
    public void setApproved(Boolean approved) { this.approved = approved; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
