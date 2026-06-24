package com.geekworkshop.finance.dto;

import com.geekworkshop.finance.entity.Budget;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BudgetResponse {

    private Long id;
    private Long departmentId;
    private String departmentName;
    private Integer budgetYear;
    private BigDecimal totalAmount;
    private BigDecimal usedAmount;
    private BigDecimal remainingAmount;
    private LocalDateTime updatedAt;

    public static BudgetResponse fromEntity(Budget budget) {
        BudgetResponse response = new BudgetResponse();
        response.id = budget.getId();
        response.budgetYear = budget.getBudgetYear();
        response.totalAmount = budget.getTotalAmount();
        response.usedAmount = budget.getUsedAmount();
        response.remainingAmount = budget.getRemainingAmount();
        response.updatedAt = budget.getUpdatedAt();
        if (budget.getDepartment() != null) {
            response.departmentId = budget.getDepartment().getId();
            response.departmentName = budget.getDepartment().getName();
        }
        return response;
    }

    public Long getId() {
        return id;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public Integer getBudgetYear() {
        return budgetYear;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getUsedAmount() {
        return usedAmount;
    }

    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
