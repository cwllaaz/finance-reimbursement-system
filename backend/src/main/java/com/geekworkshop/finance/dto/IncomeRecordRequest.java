package com.geekworkshop.finance.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public class IncomeRecordRequest {
    @NotNull
    private LocalDate receiptDate;
    @Size(max = 80)
    private String voucherNumber;
    @NotBlank
    @Size(max = 200)
    private String payerName;
    @NotBlank
    @Size(max = 80)
    private String incomeCategory;
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;
    @Size(max = 160)
    private String fundingSource;
    @Size(max = 160)
    private String arrivalAccount;
    @Size(max = 80)
    private String invoiceStatus;
    @Size(max = 1000)
    private String remark;
    private Long departmentId;

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
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
}
