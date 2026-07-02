package com.geekworkshop.finance.controller;

import com.geekworkshop.finance.dto.ApprovalRecordResponse;
import com.geekworkshop.finance.dto.ApprovalRequest;
import com.geekworkshop.finance.dto.AttachmentResponse;
import com.geekworkshop.finance.dto.InvoiceOcrRequest;
import com.geekworkshop.finance.dto.InvoiceOcrResponse;
import com.geekworkshop.finance.dto.OcrResponse;
import com.geekworkshop.finance.dto.PaymentRequest;
import com.geekworkshop.finance.dto.ReimbursementDetailResponse;
import com.geekworkshop.finance.dto.ReimbursementRequest;
import com.geekworkshop.finance.dto.ReimbursementResponse;
import com.geekworkshop.finance.dto.ReimbursementTimelineNodeResponse;
import com.geekworkshop.finance.entity.AppUser;
import com.geekworkshop.finance.entity.AttachmentType;
import com.geekworkshop.finance.entity.ReimbursementStatus;
import com.geekworkshop.finance.service.AuthService;
import com.geekworkshop.finance.service.ReimbursementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reimbursements")
public class ReimbursementController {

    private final ReimbursementService reimbursementService;
    private final AuthService authService;

    public ReimbursementController(ReimbursementService reimbursementService, AuthService authService) {
        this.reimbursementService = reimbursementService;
        this.authService = authService;
    }

    @GetMapping
    public List<ReimbursementResponse> list(
            @RequestHeader("X-Auth-Token") String token,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ReimbursementStatus status,
            @RequestParam(required = false) List<ReimbursementStatus> statuses,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo
    ) {
        AppUser currentUser = authService.requireUser(token);
        return reimbursementService.list(currentUser, keyword, status, statuses, dateFrom, dateTo);
    }

    @GetMapping("/pending")
    public List<ReimbursementResponse> pending(
            @RequestHeader("X-Auth-Token") String token,
            @RequestParam(required = false) String keyword
    ) {
        AppUser currentUser = authService.requireUser(token);
        return reimbursementService.pending(currentUser, keyword);
    }

    @GetMapping("/payment-tasks")
    public List<ReimbursementResponse> paymentTasks(@RequestHeader("X-Auth-Token") String token) {
        AppUser currentUser = authService.requireUser(token);
        return reimbursementService.paymentTasks(currentUser);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExcel(@RequestHeader("X-Auth-Token") String token) {
        AppUser currentUser = authService.requireUser(token);
        byte[] content = reimbursementService.exportExcel(currentUser);
        String filename = URLEncoder.encode(
                "报销申请数据-" + LocalDate.now() + ".xlsx",
                StandardCharsets.UTF_8
        ).replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    @GetMapping("/{id}")
    public ReimbursementResponse getById(@RequestHeader("X-Auth-Token") String token, @PathVariable Long id) {
        AppUser currentUser = authService.requireUser(token);
        return reimbursementService.getById(currentUser, id);
    }

    @GetMapping("/{id}/detail")
    public ReimbursementDetailResponse detail(@RequestHeader("X-Auth-Token") String token, @PathVariable Long id) {
        AppUser currentUser = authService.requireUser(token);
        return reimbursementService.detail(currentUser, id);
    }

    @GetMapping("/{id}/timeline")
    public List<ReimbursementTimelineNodeResponse> timeline(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long id
    ) {
        AppUser currentUser = authService.requireUser(token);
        return reimbursementService.timeline(currentUser, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReimbursementResponse create(
            @RequestHeader("X-Auth-Token") String token,
            @Valid @RequestBody ReimbursementRequest request
    ) {
        AppUser currentUser = authService.requireUser(token);
        return reimbursementService.create(currentUser, request);
    }

    @PutMapping("/{id}")
    public ReimbursementResponse update(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long id,
            @Valid @RequestBody ReimbursementRequest request
    ) {
        AppUser currentUser = authService.requireUser(token);
        return reimbursementService.update(currentUser, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader("X-Auth-Token") String token, @PathVariable Long id) {
        AppUser currentUser = authService.requireUser(token);
        reimbursementService.delete(currentUser, id);
    }

    @PostMapping("/{id}/submit")
    public ReimbursementResponse submit(@RequestHeader("X-Auth-Token") String token, @PathVariable Long id) {
        AppUser currentUser = authService.requireUser(token);
        return reimbursementService.submit(currentUser, id);
    }

    @PostMapping("/{id}/approval")
    public ReimbursementResponse approve(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long id,
            @Valid @RequestBody ApprovalRequest request
    ) {
        AppUser currentUser = authService.requireUser(token);
        return reimbursementService.approve(currentUser, id, request);
    }

    @PostMapping("/{id}/payment")
    public ReimbursementResponse confirmPayment(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long id,
            @Valid @RequestBody PaymentRequest request
    ) {
        AppUser currentUser = authService.requireUser(token);
        return reimbursementService.confirmPayment(currentUser, id, request);
    }

    @GetMapping("/{id}/approval-records")
    public List<ApprovalRecordResponse> approvalRecords(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long id
    ) {
        AppUser currentUser = authService.requireUser(token);
        return reimbursementService.approvalRecords(currentUser, id);
    }

    @PostMapping("/{id}/attachments")
    public AttachmentResponse uploadAttachment(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long id,
            @RequestParam(defaultValue = "INVOICE") AttachmentType attachmentType,
            @RequestPart("file") MultipartFile file
    ) {
        AppUser currentUser = authService.requireUser(token);
        return reimbursementService.uploadAttachment(currentUser, id, attachmentType, file);
    }

    @PostMapping("/{id}/ocr")
    public OcrResponse simulateOcr(@RequestHeader("X-Auth-Token") String token, @PathVariable Long id) {
        AppUser currentUser = authService.requireUser(token);
        return reimbursementService.simulateOcr(currentUser, id);
    }

    @PostMapping("/{id}/ocr/baidu")
    public InvoiceOcrResponse recognizeInvoice(@RequestHeader("X-Auth-Token") String token, @PathVariable Long id) {
        AppUser currentUser = authService.requireUser(token);
        return reimbursementService.recognizeInvoice(currentUser, id);
    }

    @GetMapping("/{id}/invoice-ocr")
    public InvoiceOcrResponse getInvoiceOcr(@RequestHeader("X-Auth-Token") String token, @PathVariable Long id) {
        AppUser currentUser = authService.requireUser(token);
        return reimbursementService.getInvoiceOcr(currentUser, id);
    }

    @PutMapping("/{id}/invoice-ocr")
    public InvoiceOcrResponse saveInvoiceOcr(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long id,
            @RequestBody InvoiceOcrRequest request
    ) {
        AppUser currentUser = authService.requireUser(token);
        return reimbursementService.saveInvoiceOcr(currentUser, id, request);
    }

    @PostMapping("/{id}/invoice-ocr/confirm")
    public InvoiceOcrResponse confirmInvoiceOcr(@RequestHeader("X-Auth-Token") String token, @PathVariable Long id) {
        AppUser currentUser = authService.requireUser(token);
        return reimbursementService.confirmInvoiceOcr(currentUser, id);
    }
}
