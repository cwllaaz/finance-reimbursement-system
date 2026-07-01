package com.geekworkshop.finance.dto;

import java.math.BigDecimal;
import java.util.List;

public record LedgerSummaryResponse(
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal balance,
        List<LedgerEntryResponse> entries
) {}
