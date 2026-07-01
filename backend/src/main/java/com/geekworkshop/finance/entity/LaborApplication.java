package com.geekworkshop.finance.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "labor_application")
public class LaborApplication extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_number", nullable = false, unique = true, length = 30)
    private String applicationNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private LaborCategory category;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(name = "budget_number", length = 80)
    private String budgetNumber;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "amount_in_words", nullable = false, length = 160)
    private String amountInWords;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private LaborStatus status = LaborStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private AppUser applicant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "payment_amount", precision = 14, scale = 2)
    private BigDecimal paymentAmount;

    @Column(name = "payment_voucher_number", length = 80)
    private String paymentVoucherNumber;

    @OneToMany(mappedBy = "laborApplication", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id asc")
    private List<LaborRecipient> recipients = new ArrayList<>();

    public Long getId() { return id; }
    public String getApplicationNumber() { return applicationNumber; }
    public void setApplicationNumber(String applicationNumber) { this.applicationNumber = applicationNumber; }
    public LaborCategory getCategory() { return category; }
    public void setCategory(LaborCategory category) { this.category = category; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getBudgetNumber() { return budgetNumber; }
    public void setBudgetNumber(String budgetNumber) { this.budgetNumber = budgetNumber; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getAmountInWords() { return amountInWords; }
    public void setAmountInWords(String amountInWords) { this.amountInWords = amountInWords; }
    public LaborStatus getStatus() { return status; }
    public void setStatus(LaborStatus status) { this.status = status; }
    public AppUser getApplicant() { return applicant; }
    public void setApplicant(AppUser applicant) { this.applicant = applicant; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(BigDecimal paymentAmount) { this.paymentAmount = paymentAmount; }
    public String getPaymentVoucherNumber() { return paymentVoucherNumber; }
    public void setPaymentVoucherNumber(String paymentVoucherNumber) { this.paymentVoucherNumber = paymentVoucherNumber; }
    public List<LaborRecipient> getRecipients() { return recipients; }
    public void setRecipients(List<LaborRecipient> recipients) { this.recipients = recipients; }
}
