package com.geekworkshop.finance.dto;

import com.geekworkshop.finance.entity.IncomeRecord;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record IncomeRecordResponse(
        Long id, String incomeNumber, LocalDate receiptDate, String voucherNumber,
        String payerName, String incomeCategory, BigDecimal amount, String fundingSource,
        String arrivalAccount, String invoiceStatus, String remark, Long departmentId,
        String departmentName, Long createdById, String createdByName,
        LocalDateTime createdAt, LocalDateTime updatedAt, List<IncomeAttachmentResponse> attachments
) {
    public static IncomeRecordResponse fromEntity(IncomeRecord record, List<IncomeAttachmentResponse> attachments) {
        return new IncomeRecordResponse(
                record.getId(),
                record.getIncomeNumber(),
                record.getReceiptDate(),
                record.getVoucherNumber(),
                record.getPayerName(),
                record.getIncomeCategory(),
                record.getAmount(),
                record.getFundingSource(),
                record.getArrivalAccount(),
                record.getInvoiceStatus(),
                record.getRemark(),
                record.getDepartment() == null ? null : record.getDepartment().getId(),
                record.getDepartment() == null ? null : record.getDepartment().getName(),
                record.getCreatedBy() == null ? null : record.getCreatedBy().getId(),
                record.getCreatedBy() == null ? null : record.getCreatedBy().getRealName(),
                record.getCreatedAt(),
                record.getUpdatedAt(),
                attachments
        );
    }
}
