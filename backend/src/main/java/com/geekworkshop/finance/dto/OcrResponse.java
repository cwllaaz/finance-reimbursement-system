package com.geekworkshop.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class OcrResponse {

    private String invoiceNo;
    private BigDecimal amount;
    private LocalDate invoiceDate;
    private String vendor;
    private String message;

    public OcrResponse(String invoiceNo, BigDecimal amount, LocalDate invoiceDate, String vendor, String message) {
        this.invoiceNo = invoiceNo;
        this.amount = amount;
        this.invoiceDate = invoiceDate;
        this.vendor = vendor;
        this.message = message;
    }

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public String getVendor() {
        return vendor;
    }

    public String getMessage() {
        return message;
    }
}
