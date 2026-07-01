package com.geekworkshop.finance.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class AdvanceOffsetRequest {
    @NotNull @DecimalMin("0.01") private BigDecimal amount;
    @Size(max = 500) private String comment;
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal value) { amount = value; }
    public String getComment() { return comment; }
    public void setComment(String value) { comment = value; }
}
