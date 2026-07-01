package com.geekworkshop.finance.dto;

import com.geekworkshop.finance.entity.LaborRecipient;
import java.math.BigDecimal;

public record LaborRecipientResponse(
        Long id, String name, String phone, String idCard, String organization,
        String position, String serviceContent, BigDecimal netAmount,
        String bankAccount, String bankName
) {
    public static LaborRecipientResponse fromEntity(LaborRecipient value, boolean mask) {
        return new LaborRecipientResponse(
                value.getId(), value.getName(), value.getPhone(),
                maskIdCard(value.getIdCard(), mask), value.getOrganization(), value.getPosition(),
                value.getServiceContent(), value.getNetAmount(),
                maskBankAccount(value.getBankAccount(), mask), value.getBankName()
        );
    }

    private static String maskIdCard(String value, boolean mask) {
        if (!mask || value == null || value.length() < 8) return value;
        return value.substring(0, 3) + "***********" + value.substring(value.length() - 4);
    }

    private static String maskBankAccount(String value, boolean mask) {
        if (!mask || value == null || value.length() < 8) return value;
        return value.substring(0, 4) + " **** **** " + value.substring(value.length() - 4);
    }
}
