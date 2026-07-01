package com.geekworkshop.finance.dto;

import com.geekworkshop.finance.entity.Reimbursement;
import com.geekworkshop.finance.entity.ReimbursementStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ReimbursementResponse {

    private Long id;
    private String approvalNumber;
    private String title;
    private String expenseType;
    private BigDecimal amount;
    private LocalDate expenseDate;
    private String description;
    private String applicantPhone;
    private String budgetNumber;
    private String reimbursementReason;
    private LocalDate paymentDate;
    private String payeeName;
    private String bankAccount;
    private String bankName;
    private BigDecimal paymentTotal;
    private String paymentVoucherNumber;
    private String relatedPurchaseNumber;
    private String highValueExplanation;
    private ReimbursementStatus status;
    private Long applicantId;
    private String applicantName;
    private Long departmentId;
    private String departmentName;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ReimbursementResponse fromEntity(Reimbursement reimbursement) {
        ReimbursementResponse response = new ReimbursementResponse();
        response.id = reimbursement.getId();
        response.approvalNumber = reimbursement.getApprovalNumber();
        response.title = reimbursement.getTitle();
        response.expenseType = reimbursement.getExpenseType();
        response.amount = reimbursement.getAmount();
        response.expenseDate = reimbursement.getExpenseDate();
        response.description = reimbursement.getDescription();
        response.applicantPhone = reimbursement.getApplicantPhone();
        response.budgetNumber = reimbursement.getBudgetNumber();
        response.reimbursementReason = reimbursement.getReimbursementReason();
        response.paymentDate = reimbursement.getPaymentDate();
        response.payeeName = reimbursement.getPayeeName();
        response.bankAccount = reimbursement.getBankAccount();
        response.bankName = reimbursement.getBankName();
        response.paymentTotal = reimbursement.getPaymentTotal();
        response.paymentVoucherNumber = reimbursement.getPaymentVoucherNumber();
        response.relatedPurchaseNumber = reimbursement.getRelatedPurchaseNumber();
        response.highValueExplanation = reimbursement.getHighValueExplanation();
        response.status = reimbursement.getStatus();
        response.submittedAt = reimbursement.getSubmittedAt();
        response.createdAt = reimbursement.getCreatedAt();
        response.updatedAt = reimbursement.getUpdatedAt();

        if (reimbursement.getApplicant() != null) {
            response.applicantId = reimbursement.getApplicant().getId();
            response.applicantName = reimbursement.getApplicant().getRealName();
        }

        if (reimbursement.getDepartment() != null) {
            response.departmentId = reimbursement.getDepartment().getId();
            response.departmentName = reimbursement.getDepartment().getName();
        }

        return response;
    }

    public Long getId() {
        return id;
    }

    public String getApprovalNumber() {
        return approvalNumber;
    }

    public String getTitle() {
        return title;
    }

    public String getExpenseType() {
        return expenseType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public String getDescription() {
        return description;
    }

    public String getApplicantPhone() {
        return applicantPhone;
    }

    public String getBudgetNumber() {
        return budgetNumber;
    }

    public String getReimbursementReason() {
        return reimbursementReason;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public String getPayeeName() {
        return payeeName;
    }

    public String getBankAccount() {
        return bankAccount;
    }

    public String getBankName() {
        return bankName;
    }

    public BigDecimal getPaymentTotal() {
        return paymentTotal;
    }

    public String getPaymentVoucherNumber() {
        return paymentVoucherNumber;
    }

    public String getRelatedPurchaseNumber() {
        return relatedPurchaseNumber;
    }

    public String getHighValueExplanation() {
        return highValueExplanation;
    }

    public ReimbursementStatus getStatus() {
        return status;
    }

    public Long getApplicantId() {
        return applicantId;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
