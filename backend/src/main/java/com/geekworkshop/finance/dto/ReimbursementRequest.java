package com.geekworkshop.finance.dto;

import com.geekworkshop.finance.entity.ReimbursementStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReimbursementRequest {

    private Long applicantId;

    private Long departmentId;

    @NotBlank(message = "title is required")
    @Size(max = 120, message = "title is too long")
    private String title;

    @NotBlank(message = "expenseType is required")
    @Size(max = 60, message = "expenseType is too long")
    private String expenseType;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "expenseDate is required")
    private LocalDate expenseDate;

    @Size(max = 500, message = "description is too long")
    private String description;

    @Size(max = 40, message = "applicantPhone is too long")
    private String applicantPhone;

    @Size(max = 60, message = "budgetNumber is too long")
    private String budgetNumber;

    @Size(max = 500, message = "reimbursementReason is too long")
    private String reimbursementReason;

    private LocalDate paymentDate;

    @Size(max = 160, message = "payeeName is too long")
    private String payeeName;

    @Size(max = 80, message = "bankAccount is too long")
    private String bankAccount;

    @Size(max = 160, message = "bankName is too long")
    private String bankName;

    @DecimalMin(value = "0.00", message = "paymentTotal cannot be negative")
    private BigDecimal paymentTotal;

    @Size(max = 60, message = "relatedPurchaseNumber is too long")
    private String relatedPurchaseNumber;

    @Size(max = 1000, message = "highValueExplanation is too long")
    private String highValueExplanation;

    private ReimbursementStatus status;

    public Long getApplicantId() {
        return applicantId;
    }

    public void setApplicantId(Long applicantId) {
        this.applicantId = applicantId;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getExpenseType() {
        return expenseType;
    }

    public void setExpenseType(String expenseType) {
        this.expenseType = expenseType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDate expenseDate) {
        this.expenseDate = expenseDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getApplicantPhone() {
        return applicantPhone;
    }

    public void setApplicantPhone(String applicantPhone) {
        this.applicantPhone = applicantPhone;
    }

    public String getBudgetNumber() {
        return budgetNumber;
    }

    public void setBudgetNumber(String budgetNumber) {
        this.budgetNumber = budgetNumber;
    }

    public String getReimbursementReason() {
        return reimbursementReason;
    }

    public void setReimbursementReason(String reimbursementReason) {
        this.reimbursementReason = reimbursementReason;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getPayeeName() {
        return payeeName;
    }

    public void setPayeeName(String payeeName) {
        this.payeeName = payeeName;
    }

    public String getBankAccount() {
        return bankAccount;
    }

    public void setBankAccount(String bankAccount) {
        this.bankAccount = bankAccount;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public BigDecimal getPaymentTotal() {
        return paymentTotal;
    }

    public void setPaymentTotal(BigDecimal paymentTotal) {
        this.paymentTotal = paymentTotal;
    }

    public String getRelatedPurchaseNumber() {
        return relatedPurchaseNumber;
    }

    public void setRelatedPurchaseNumber(String relatedPurchaseNumber) {
        this.relatedPurchaseNumber = relatedPurchaseNumber;
    }

    public String getHighValueExplanation() {
        return highValueExplanation;
    }

    public void setHighValueExplanation(String highValueExplanation) {
        this.highValueExplanation = highValueExplanation;
    }

    public ReimbursementStatus getStatus() {
        return status;
    }

    public void setStatus(ReimbursementStatus status) {
        this.status = status;
    }
}
