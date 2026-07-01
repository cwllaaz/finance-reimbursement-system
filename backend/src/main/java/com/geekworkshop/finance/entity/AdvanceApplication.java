package com.geekworkshop.finance.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "advance_application")
public class AdvanceApplication extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "application_number", nullable = false, unique = true, length = 30)
    private String applicationNumber;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private AdvanceType type;
    @Column(nullable = false, length = 1000)
    private String reason;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;
    @Column(name = "payment_method", nullable = false, length = 60)
    private String paymentMethod;
    @Column(name = "payee_name", nullable = false, length = 160)
    private String payeeName;
    @Column(name = "bank_account", nullable = false, length = 80)
    private String bankAccount;
    @Column(name = "bank_name", nullable = false, length = 160)
    private String bankName;
    @Column(name = "expected_repayment_date")
    private LocalDate expectedRepaymentDate;
    @Column(name = "partner_name", length = 200)
    private String partnerName;
    @Column(name = "expected_settlement_date")
    private LocalDate expectedSettlementDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private AdvanceStatus status = AdvanceStatus.DRAFT;
    @Enumerated(EnumType.STRING) @Column(name = "settlement_status", length = 40)
    private SettlementStatus settlementStatus;
    @Column(name = "offset_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal offsetAmount = BigDecimal.ZERO;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "applicant_id", nullable = false)
    private AppUser applicant;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "department_id")
    private Department department;
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
    @Column(name = "payment_date")
    private LocalDate paymentDate;
    @Column(name = "payment_amount", precision = 14, scale = 2)
    private BigDecimal paymentAmount;
    @Column(name = "payment_voucher_number", length = 80)
    private String paymentVoucherNumber;

    public Long getId() { return id; }
    public String getApplicationNumber() { return applicationNumber; }
    public void setApplicationNumber(String value) { applicationNumber = value; }
    public AdvanceType getType() { return type; }
    public void setType(AdvanceType value) { type = value; }
    public String getReason() { return reason; }
    public void setReason(String value) { reason = value; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal value) { amount = value; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String value) { paymentMethod = value; }
    public String getPayeeName() { return payeeName; }
    public void setPayeeName(String value) { payeeName = value; }
    public String getBankAccount() { return bankAccount; }
    public void setBankAccount(String value) { bankAccount = value; }
    public String getBankName() { return bankName; }
    public void setBankName(String value) { bankName = value; }
    public LocalDate getExpectedRepaymentDate() { return expectedRepaymentDate; }
    public void setExpectedRepaymentDate(LocalDate value) { expectedRepaymentDate = value; }
    public String getPartnerName() { return partnerName; }
    public void setPartnerName(String value) { partnerName = value; }
    public LocalDate getExpectedSettlementDate() { return expectedSettlementDate; }
    public void setExpectedSettlementDate(LocalDate value) { expectedSettlementDate = value; }
    public AdvanceStatus getStatus() { return status; }
    public void setStatus(AdvanceStatus value) { status = value; }
    public SettlementStatus getSettlementStatus() { return settlementStatus; }
    public void setSettlementStatus(SettlementStatus value) { settlementStatus = value; }
    public BigDecimal getOffsetAmount() { return offsetAmount; }
    public void setOffsetAmount(BigDecimal value) { offsetAmount = value; }
    public AppUser getApplicant() { return applicant; }
    public void setApplicant(AppUser value) { applicant = value; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department value) { department = value; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime value) { submittedAt = value; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate value) { paymentDate = value; }
    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(BigDecimal value) { paymentAmount = value; }
    public String getPaymentVoucherNumber() { return paymentVoucherNumber; }
    public void setPaymentVoucherNumber(String value) { paymentVoucherNumber = value; }
}
