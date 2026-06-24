package com.geekworkshop.finance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "invoice_ocr_result")
public class InvoiceOcrResult extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reimbursement_id", nullable = false)
    private Reimbursement reimbursement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attachment_id")
    private Attachment attachment;

    @Column(name = "invoice_code", length = 80)
    private String invoiceCode;

    @Column(name = "invoice_number", length = 80)
    private String invoiceNumber;

    @Column(name = "invoice_date")
    private LocalDate invoiceDate;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "tax_amount", precision = 12, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "amount_matched")
    private Boolean amountMatched;

    @Column(name = "amount_difference", precision = 12, scale = 2)
    private BigDecimal amountDifference;

    @Column(name = "verification_message", length = 200)
    private String verificationMessage;

    @Column(name = "seller_name", length = 200)
    private String sellerName;

    @Column(name = "buyer_name", length = 200)
    private String buyerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "ocr_status", nullable = false, length = 40)
    private InvoiceOcrStatus ocrStatus = InvoiceOcrStatus.UNRECOGNIZED;

    @Lob
    @Column(name = "raw_ocr_json", columnDefinition = "TEXT")
    private String rawOcrJson;

    public Long getId() {
        return id;
    }

    public Reimbursement getReimbursement() {
        return reimbursement;
    }

    public void setReimbursement(Reimbursement reimbursement) {
        this.reimbursement = reimbursement;
    }

    public Attachment getAttachment() {
        return attachment;
    }

    public void setAttachment(Attachment attachment) {
        this.attachment = attachment;
    }

    public String getInvoiceCode() {
        return invoiceCode;
    }

    public void setInvoiceCode(String invoiceCode) {
        this.invoiceCode = invoiceCode;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(LocalDate invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public Boolean getAmountMatched() {
        return amountMatched;
    }

    public void setAmountMatched(Boolean amountMatched) {
        this.amountMatched = amountMatched;
    }

    public BigDecimal getAmountDifference() {
        return amountDifference;
    }

    public void setAmountDifference(BigDecimal amountDifference) {
        this.amountDifference = amountDifference;
    }

    public String getVerificationMessage() {
        return verificationMessage;
    }

    public void setVerificationMessage(String verificationMessage) {
        this.verificationMessage = verificationMessage;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }

    public InvoiceOcrStatus getOcrStatus() {
        return ocrStatus;
    }

    public void setOcrStatus(InvoiceOcrStatus ocrStatus) {
        this.ocrStatus = ocrStatus;
    }

    public String getRawOcrJson() {
        return rawOcrJson;
    }

    public void setRawOcrJson(String rawOcrJson) {
        this.rawOcrJson = rawOcrJson;
    }
}
