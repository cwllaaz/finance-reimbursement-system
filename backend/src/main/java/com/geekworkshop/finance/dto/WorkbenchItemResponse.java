package com.geekworkshop.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WorkbenchItemResponse(
        String businessType,
        Long businessId,
        String number,
        String title,
        Long applicantId,
        String applicantName,
        Long departmentId,
        String departmentName,
        BigDecimal amount,
        String status,
        LocalDateTime time
) {
}
