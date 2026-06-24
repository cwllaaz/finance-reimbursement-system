package com.geekworkshop.finance.dto;

import com.geekworkshop.finance.entity.InvoiceOcrResult;
import com.geekworkshop.finance.entity.InvoiceOcrStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class InvoiceOcrResponse {

    private Long id;
    private Long reimbursementId;
    private Long attachmentId;
    private String invoiceCode;
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private BigDecimal amount;
    private BigDecimal taxAmount;
    private Boolean amountMatched;
    private BigDecimal amountDifference;
    private String verificationMessage;
    private String sellerName;
    private String buyerName;
    private InvoiceOcrStatus ocrStatus;
    private LocalDateTime updatedAt;

    public static InvoiceOcrResponse fromEntity(InvoiceOcrResult result) {
        InvoiceOcrResponse response = new InvoiceOcrResponse();
        response.id = result.getId();
        response.reimbursementId = result.getReimbursement().getId();
        response.attachmentId = result.getAttachment() == null ? null : result.getAttachment().getId();
        response.invoiceCode = result.getInvoiceCode();
        response.invoiceNumber = result.getInvoiceNumber();
        response.invoiceDate = result.getInvoiceDate();
        response.amount = result.getAmount();
        response.taxAmount = result.getTaxAmount();
        response.amountMatched = result.getAmountMatched();
        response.amountDifference = result.getAmountDifference();
        response.verificationMessage = result.getVerificationMessage();
        response.sellerName = result.getSellerName();
        response.buyerName = result.getBuyerName();
        response.ocrStatus = result.getOcrStatus();
        response.updatedAt = result.getUpdatedAt();
        return response;
    }

    public Long getId() {
        return id;
    }

    public Long getReimbursementId() {
        return reimbursementId;
    }

    public Long getAttachmentId() {
        return attachmentId;
    }

    public String getInvoiceCode() {
        return invoiceCode;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public Boolean getAmountMatched() {
        return amountMatched;
    }

    public BigDecimal getAmountDifference() {
        return amountDifference;
    }

    public String getVerificationMessage() {
        return verificationMessage;
    }

    public String getSellerName() {
        return sellerName;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public InvoiceOcrStatus getOcrStatus() {
        return ocrStatus;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
