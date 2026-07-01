package com.geekworkshop.finance.dto;

import com.geekworkshop.finance.entity.LaborCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public class LaborApplicationRequest {
    @NotNull private LaborCategory category;
    @NotBlank @Size(max = 200) private String title;
    @Size(max = 1000) private String description;
    @Size(max = 80) private String budgetNumber;
    @NotEmpty @Valid private List<LaborRecipientRequest> recipients;

    public LaborCategory getCategory() { return category; }
    public void setCategory(LaborCategory category) { this.category = category; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getBudgetNumber() { return budgetNumber; }
    public void setBudgetNumber(String budgetNumber) { this.budgetNumber = budgetNumber; }
    public List<LaborRecipientRequest> getRecipients() { return recipients; }
    public void setRecipients(List<LaborRecipientRequest> recipients) { this.recipients = recipients; }
}
