package com.geekworkshop.finance.dto;

import java.util.List;

public class ReimbursementDetailResponse {

    private ReimbursementResponse reimbursement;
    private List<ApprovalRecordResponse> approvalRecords;
    private List<AttachmentResponse> attachments;
    private InvoiceOcrResponse invoiceOcr;

    public ReimbursementDetailResponse(
            ReimbursementResponse reimbursement,
            List<ApprovalRecordResponse> approvalRecords,
            List<AttachmentResponse> attachments,
            InvoiceOcrResponse invoiceOcr
    ) {
        this.reimbursement = reimbursement;
        this.approvalRecords = approvalRecords;
        this.attachments = attachments;
        this.invoiceOcr = invoiceOcr;
    }

    public ReimbursementResponse getReimbursement() {
        return reimbursement;
    }

    public List<ApprovalRecordResponse> getApprovalRecords() {
        return approvalRecords;
    }

    public List<AttachmentResponse> getAttachments() {
        return attachments;
    }

    public InvoiceOcrResponse getInvoiceOcr() {
        return invoiceOcr;
    }
}
