package com.geekworkshop.finance.dto;

import com.geekworkshop.finance.entity.PurchaseItem;
import java.math.BigDecimal;

public record PurchaseItemResponse(
        Long id, String itemName, String specification, String manufacturer,
        BigDecimal unitPrice, Integer quantity, BigDecimal totalPrice
) {
    public static PurchaseItemResponse fromEntity(PurchaseItem item) {
        return new PurchaseItemResponse(item.getId(), item.getItemName(), item.getSpecification(),
                item.getManufacturer(), item.getUnitPrice(), item.getQuantity(), item.getTotalPrice());
    }
}
