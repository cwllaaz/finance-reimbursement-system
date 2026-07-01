package com.geekworkshop.finance.controller;

import com.geekworkshop.finance.dto.*;
import com.geekworkshop.finance.entity.AppUser;
import com.geekworkshop.finance.service.AuthService;
import com.geekworkshop.finance.service.IncomeLedgerService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
public class IncomeLedgerController {
    private final IncomeLedgerService incomeLedgerService;
    private final AuthService authService;

    public IncomeLedgerController(IncomeLedgerService incomeLedgerService, AuthService authService) {
        this.incomeLedgerService = incomeLedgerService;
        this.authService = authService;
    }

    @GetMapping("/api/incomes")
    public List<IncomeRecordResponse> incomes(
            @RequestHeader("X-Auth-Token") String token,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {
        return incomeLedgerService.listIncomes(authService.requireUser(token), keyword, startDate, endDate);
    }

    @GetMapping("/api/incomes/{id}")
    public IncomeRecordResponse incomeDetail(@RequestHeader("X-Auth-Token") String token, @PathVariable Long id) {
        return incomeLedgerService.detail(authService.requireUser(token), id);
    }

    @PostMapping("/api/incomes")
    @ResponseStatus(HttpStatus.CREATED)
    public IncomeRecordResponse createIncome(
            @RequestHeader("X-Auth-Token") String token,
            @Valid @RequestBody IncomeRecordRequest request
    ) {
        return incomeLedgerService.create(authService.requireUser(token), request);
    }

    @PutMapping("/api/incomes/{id}")
    public IncomeRecordResponse updateIncome(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long id,
            @Valid @RequestBody IncomeRecordRequest request
    ) {
        return incomeLedgerService.update(authService.requireUser(token), id, request);
    }

    @DeleteMapping("/api/incomes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteIncome(@RequestHeader("X-Auth-Token") String token, @PathVariable Long id) {
        incomeLedgerService.delete(authService.requireUser(token), id);
    }

    @PostMapping("/api/incomes/{id}/attachments")
    public IncomeAttachmentResponse uploadIncomeAttachment(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file
    ) {
        return incomeLedgerService.uploadIncomeAttachment(authService.requireUser(token), id, file);
    }

    @GetMapping("/api/ledger")
    public LedgerSummaryResponse ledger(
            @RequestHeader("X-Auth-Token") String token,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String businessType
    ) {
        AppUser currentUser = authService.requireUser(token);
        return incomeLedgerService.ledger(currentUser, startDate, endDate, departmentId, businessType);
    }

    @GetMapping("/api/ledger/export")
    public ResponseEntity<byte[]> exportLedger(
            @RequestHeader("X-Auth-Token") String token,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String businessType
    ) {
        AppUser currentUser = authService.requireUser(token);
        byte[] content = incomeLedgerService.exportLedger(currentUser, startDate, endDate, departmentId, businessType);
        String filename = URLEncoder.encode("单位收支总台账-" + LocalDate.now() + ".xlsx", StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }
}
