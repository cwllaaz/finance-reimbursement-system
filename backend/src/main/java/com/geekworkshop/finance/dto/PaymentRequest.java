package com.geekworkshop.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PaymentRequest {

    @NotNull(message = "paymentDate is required")
    private LocalDate paymentDate;

    @NotNull(message = "paymentAmount is required")
    @DecimalMin(value = "0.01", message = "paymentAmount must be greater than 0")
    private BigDecimal paymentAmount;

    @NotBlank(message = "voucherNumber is required")
    @Size(max = 80, message = "voucherNumber is too long")
    private String voucherNumber;

    @Size(max = 500, message = "comment is too long")
    private String comment;

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(BigDecimal paymentAmount) { this.paymentAmount = paymentAmount; }
    public String getVoucherNumber() { return voucherNumber; }
    public void setVoucherNumber(String voucherNumber) { this.voucherNumber = voucherNumber; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
