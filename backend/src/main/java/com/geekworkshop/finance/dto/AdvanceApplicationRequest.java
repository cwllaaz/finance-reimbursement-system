package com.geekworkshop.finance.dto;

import com.geekworkshop.finance.entity.AdvanceType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public class AdvanceApplicationRequest {
    @NotNull private AdvanceType type;
    @NotBlank @Size(max = 1000) private String reason;
    @NotNull @DecimalMin("0.01") private BigDecimal amount;
    @NotBlank @Size(max = 60) private String paymentMethod;
    @NotBlank @Size(max = 160) private String payeeName;
    @NotBlank @Size(max = 80) private String bankAccount;
    @NotBlank @Size(max = 160) private String bankName;
    private LocalDate expectedRepaymentDate;
    @Size(max = 200) private String partnerName;
    private LocalDate expectedSettlementDate;
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
}
