package com.geekworkshop.finance.dto;

import com.geekworkshop.finance.entity.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public record AdvanceApplicationResponse(
        Long id, String applicationNumber, AdvanceType type, String reason, BigDecimal amount,
        String paymentMethod, String payeeName, String bankAccount, String bankName,
        LocalDate expectedRepaymentDate, String partnerName, LocalDate expectedSettlementDate,
        AdvanceStatus status, SettlementStatus settlementStatus, BigDecimal offsetAmount,
        BigDecimal remainingAmount, Long applicantId, String applicantName, String departmentName,
        LocalDateTime submittedAt, LocalDate paymentDate, BigDecimal paymentAmount,
        String paymentVoucherNumber, LocalDateTime createdAt,
        List<AdvanceAttachmentResponse> attachments, List<AdvanceOffsetResponse> offsetRecords,
        List<AdvanceTimelineResponse> timeline
) {}
