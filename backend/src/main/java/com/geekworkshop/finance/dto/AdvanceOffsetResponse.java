package com.geekworkshop.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdvanceOffsetResponse(
        Long id, BigDecimal amount, String comment, String operatorName, LocalDateTime createdAt
) {}
