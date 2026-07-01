package com.geekworkshop.finance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reimbursement")
public class Reimbursement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "approval_number", unique = true, length = 32)
    private String approvalNumber;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(name = "expense_type", nullable = false, length = 60)
    private String expenseType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(length = 500)
    private String description;

    @Column(name = "applicant_phone", length = 40)
    private String applicantPhone;

    @Column(name = "budget_number", length = 60)
    private String budgetNumber;

    @Column(name = "reimbursement_reason", length = 500)
    private String reimbursementReason;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "payee_name", length = 160)
    private String payeeName;

    @Column(name = "bank_account", length = 80)
    private String bankAccount;

    @Column(name = "bank_name", length = 160)
    private String bankName;

    @Column(name = "payment_total", precision = 12, scale = 2)
    private BigDecimal paymentTotal;

    @Column(name = "payment_voucher_number", length = 80)
    private String paymentVoucherNumber;

    @Column(name = "related_purchase_number", length = 40)
    private String relatedPurchaseNumber;

    @Column(name = "high_value_explanation", length = 1000)
    private String highValueExplanation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ReimbursementStatus status = ReimbursementStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id")
    private AppUser applicant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    public Long getId() {
        return id;
    }

    public String getApprovalNumber() {
        return approvalNumber;
    }

    public void setApprovalNumber(String approvalNumber) {
        this.approvalNumber = approvalNumber;
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

    public String getPaymentVoucherNumber() {
        return paymentVoucherNumber;
    }

    public void setPaymentVoucherNumber(String paymentVoucherNumber) {
        this.paymentVoucherNumber = paymentVoucherNumber;
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

    public AppUser getApplicant() {
        return applicant;
    }

    public void setApplicant(AppUser applicant) {
        this.applicant = applicant;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
}
