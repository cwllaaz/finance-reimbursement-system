package com.geekworkshop.finance.dto;

import com.geekworkshop.finance.entity.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public record LaborApplicationResponse(
        Long id, String applicationNumber, LaborCategory category, String title,
        String description, String budgetNumber, BigDecimal totalAmount, String amountInWords,
        LaborStatus status, Long applicantId, String applicantName, Long departmentId,
        String departmentName, LocalDateTime submittedAt, LocalDate paymentDate,
        BigDecimal paymentAmount, String paymentVoucherNumber, LocalDateTime createdAt,
        List<LaborRecipientResponse> recipients, List<LaborAttachmentResponse> attachments,
        List<LaborTimelineResponse> timeline
) {}
