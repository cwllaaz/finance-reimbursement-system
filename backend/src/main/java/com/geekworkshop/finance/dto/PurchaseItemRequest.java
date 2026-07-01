package com.geekworkshop.finance.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class PurchaseItemRequest {
    @NotBlank @Size(max = 200)
    private String itemName;
    @Size(max = 200)
    private String specification;
    @Size(max = 200)
    private String manufacturer;
    @NotNull @DecimalMin(value = "0.01")
    private BigDecimal unitPrice;
    @NotNull @Min(1)
    private Integer quantity;

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getSpecification() { return specification; }
    public void setSpecification(String specification) { this.specification = specification; }
    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
