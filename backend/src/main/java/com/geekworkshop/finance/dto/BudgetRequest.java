package com.geekworkshop.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class BudgetRequest {

    @NotNull(message = "totalAmount is required")
    @DecimalMin(value = "0.00", message = "totalAmount cannot be negative")
    private BigDecimal totalAmount;

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}
