package com.geekworkshop.finance.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class DashboardStatsResponse {

    private BigDecimal monthAmount;
    private Long pendingCount;
    private Long reimbursementCount;
    private Map<String, Long> statusCounts;
    private List<BudgetResponse> budgets;
    private Long pendingOffsetCount;
    private Long overdueAdvanceCount;

    public DashboardStatsResponse(
            BigDecimal monthAmount,
            Long pendingCount,
            Long reimbursementCount,
            Map<String, Long> statusCounts,
            List<BudgetResponse> budgets
    ) {
        this.monthAmount = monthAmount;
        this.pendingCount = pendingCount;
        this.reimbursementCount = reimbursementCount;
        this.statusCounts = statusCounts;
        this.budgets = budgets;
        this.pendingOffsetCount = 0L;
        this.overdueAdvanceCount = 0L;
    }

    public BigDecimal getMonthAmount() {
        return monthAmount;
    }

    public Long getPendingCount() {
        return pendingCount;
    }

    public Long getReimbursementCount() {
        return reimbursementCount;
    }

    public Map<String, Long> getStatusCounts() {
        return statusCounts;
    }

    public List<BudgetResponse> getBudgets() {
        return budgets;
    }

    public Long getPendingOffsetCount() { return pendingOffsetCount; }
    public void setPendingOffsetCount(Long value) { pendingOffsetCount = value; }
    public Long getOverdueAdvanceCount() { return overdueAdvanceCount; }
    public void setOverdueAdvanceCount(Long value) { overdueAdvanceCount = value; }
}
