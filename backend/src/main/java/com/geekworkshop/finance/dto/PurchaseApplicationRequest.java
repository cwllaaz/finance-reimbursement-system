package com.geekworkshop.finance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.ArrayList;
import java.util.List;

public class PurchaseApplicationRequest {
    @Size(max = 40)
    private String applicantPhone;
    @Size(max = 80)
    private String budgetNumber;
    @NotBlank @Size(max = 80)
    private String purchaseMethod;
    @NotNull
    private Boolean taxExempt = false;
    @Size(max = 200)
    private String useLocation;
    @NotBlank @Size(max = 1000)
    private String purchaseReason;
    @Size(max = 80)
    private String assetAcceptanceNumber;
    @NotEmpty
    private List<@Valid PurchaseItemRequest> items = new ArrayList<>();

    public String getApplicantPhone() { return applicantPhone; }
    public void setApplicantPhone(String applicantPhone) { this.applicantPhone = applicantPhone; }
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
    public List<PurchaseItemRequest> getItems() { return items; }
    public void setItems(List<PurchaseItemRequest> items) { this.items = items; }
}
