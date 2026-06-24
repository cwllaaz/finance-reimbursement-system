package com.geekworkshop.finance.dto;

import com.geekworkshop.finance.entity.Reimbursement;
import com.geekworkshop.finance.entity.ReimbursementStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ReimbursementResponse {

    private Long id;
    private String title;
    private String expenseType;
    private BigDecimal amount;
    private LocalDate expenseDate;
    private String description;
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
        response.title = reimbursement.getTitle();
        response.expenseType = reimbursement.getExpenseType();
        response.amount = reimbursement.getAmount();
        response.expenseDate = reimbursement.getExpenseDate();
        response.description = reimbursement.getDescription();
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
