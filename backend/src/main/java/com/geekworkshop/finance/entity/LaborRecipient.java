package com.geekworkshop.finance.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "labor_recipient")
public class LaborRecipient extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "labor_application_id", nullable = false)
    private LaborApplication laborApplication;
    @Column(nullable = false, length = 80)
    private String name;
    @Column(length = 40)
    private String phone;
    @Column(name = "id_card", nullable = false, length = 40)
    private String idCard;
    @Column(length = 160)
    private String organization;
    @Column(length = 100)
    private String position;
    @Column(name = "service_content", nullable = false, length = 500)
    private String serviceContent;
    @Column(name = "net_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal netAmount;
    @Column(name = "bank_account", nullable = false, length = 80)
    private String bankAccount;
    @Column(name = "bank_name", nullable = false, length = 160)
    private String bankName;

    public Long getId() { return id; }
    public LaborApplication getLaborApplication() { return laborApplication; }
    public void setLaborApplication(LaborApplication laborApplication) { this.laborApplication = laborApplication; }
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
