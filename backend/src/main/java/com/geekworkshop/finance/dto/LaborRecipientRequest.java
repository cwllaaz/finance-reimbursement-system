package com.geekworkshop.finance.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class LaborRecipientRequest {
    @NotBlank @Size(max = 80) private String name;
    @Size(max = 40) private String phone;
    @NotBlank @Size(max = 40) private String idCard;
    @Size(max = 160) private String organization;
    @Size(max = 100) private String position;
    @NotBlank @Size(max = 500) private String serviceContent;
    @NotNull @DecimalMin("0.01") private BigDecimal netAmount;
    @NotBlank @Size(max = 80) private String bankAccount;
    @NotBlank @Size(max = 160) private String bankName;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }
    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public String getServiceContent() { return serviceContent; }
    public void setServiceContent(String serviceContent) { this.serviceContent = serviceContent; }
    public BigDecimal getNetAmount() { return netAmount; }
    public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }
    public String getBankAccount() { return bankAccount; }
    public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
}
