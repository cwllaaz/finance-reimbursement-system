package com.geekworkshop.finance.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "income_record")
public class IncomeRecord extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "income_number", nullable = false, unique = true, length = 30)
    private String incomeNumber;

    @Column(name = "receipt_date", nullable = false)
    private LocalDate receiptDate;

    @Column(name = "voucher_number", length = 80)
    private String voucherNumber;

    @Column(name = "payer_name", nullable = false, length = 200)
    private String payerName;

    @Column(name = "income_category", nullable = false, length = 80)
    private String incomeCategory;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "funding_source", length = 160)
    private String fundingSource;

    @Column(name = "arrival_account", length = 160)
    private String arrivalAccount;

    @Column(name = "invoice_status", length = 80)
    private String invoiceStatus;

    @Column(length = 1000)
    private String remark;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private AppUser createdBy;

    public Long getId() { return id; }
    public String getIncomeNumber() { return incomeNumber; }
    public void setIncomeNumber(String incomeNumber) { this.incomeNumber = incomeNumber; }
    public LocalDate getReceiptDate() { return receiptDate; }
    public void setReceiptDate(LocalDate receiptDate) { this.receiptDate = receiptDate; }
    public String getVoucherNumber() { return voucherNumber; }
    public void setVoucherNumber(String voucherNumber) { this.voucherNumber = voucherNumber; }
    public String getPayerName() { return payerName; }
    public void setPayerName(String payerName) { this.payerName = payerName; }
    public String getIncomeCategory() { return incomeCategory; }
    public void setIncomeCategory(String incomeCategory) { this.incomeCategory = incomeCategory; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getFundingSource() { return fundingSource; }
    public void setFundingSource(String fundingSource) { this.fundingSource = fundingSource; }
    public String getArrivalAccount() { return arrivalAccount; }
    public void setArrivalAccount(String arrivalAccount) { this.arrivalAccount = arrivalAccount; }
    public String getInvoiceStatus() { return invoiceStatus; }
    public void setInvoiceStatus(String invoiceStatus) { this.invoiceStatus = invoiceStatus; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public AppUser getCreatedBy() { return createdBy; }
    public void setCreatedBy(AppUser createdBy) { this.createdBy = createdBy; }
}
