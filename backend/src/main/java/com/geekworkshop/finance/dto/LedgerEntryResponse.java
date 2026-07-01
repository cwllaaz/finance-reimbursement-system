package com.geekworkshop.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LedgerEntryResponse(
        LocalDate businessDate,
        String direction,
        String businessType,
        String businessNumber,
        Long departmentId,
        String departmentName,
        String operatorName,
        String summary,
        BigDecimal incomeAmount,
        BigDecimal expenseAmount,
        String remark
) {}
