package com.geekworkshop.finance.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_application")
public class PurchaseApplication extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_number", nullable = false, unique = true, length = 30)
    private String applicationNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private AppUser applicant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "applicant_phone", length = 40)
    private String applicantPhone;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "budget_number", length = 80)
    private String budgetNumber;

    @Column(name = "purchase_method", nullable = false, length = 80)
    private String purchaseMethod;

    @Column(name = "tax_exempt", nullable = false)
    private Boolean taxExempt = false;

    @Column(name = "use_location", length = 200)
    private String useLocation;

    @Column(name = "purchase_reason", nullable = false, length = 1000)
    private String purchaseReason;

    @Column(name = "asset_acceptance_number", length = 80)
    private String assetAcceptanceNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PurchaseStatus status = PurchaseStatus.DRAFT;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @OneToMany(mappedBy = "purchaseApplication", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id asc")
    private List<PurchaseItem> items = new ArrayList<>();

    public Long getId() { return id; }
    public String getApplicationNumber() { return applicationNumber; }
    public void setApplicationNumber(String applicationNumber) { this.applicationNumber = applicationNumber; }
    public AppUser getApplicant() { return applicant; }
    public void setApplicant(AppUser applicant) { this.applicant = applicant; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public String getApplicantPhone() { return applicantPhone; }
    public void setApplicantPhone(String applicantPhone) { this.applicantPhone = applicantPhone; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getBudgetNumber() { return budgetNumber; }
    public void setBudgetNumber(String budgetNumber) { this.budgetNumber = budgetNumber; }
    public String getPurchaseMethod() { return purchaseMethod; }
    public void setPurchaseMethod(String purchaseMethod) { this.purchaseMethod = purchaseMethod; }
    public Boolean getTaxExempt() { return taxExempt; }
    public void setTaxExempt(Boolean taxExempt) { this.taxExempt = taxExempt; }
    public String getUseLocation() { return useLocation; }
    public void setUseLocation(String useLocation) { this.useLocation = useLocation; }
    public String getPurchaseReason() { return purchaseReason; }
    public void setPurchaseReason(String purchaseReason) { this.purchaseReason = purchaseReason; }
    public String getAssetAcceptanceNumber() { return assetAcceptanceNumber; }
    public void setAssetAcceptanceNumber(String assetAcceptanceNumber) { this.assetAcceptanceNumber = assetAcceptanceNumber; }
    public PurchaseStatus getStatus() { return status; }
    public void setStatus(PurchaseStatus status) { this.status = status; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public List<PurchaseItem> getItems() { return items; }
    public void setItems(List<PurchaseItem> items) { this.items = items; }
}
