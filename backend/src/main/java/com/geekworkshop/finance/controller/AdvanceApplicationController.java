package com.geekworkshop.finance.controller;

import com.geekworkshop.finance.dto.*;
import com.geekworkshop.finance.entity.*;
import com.geekworkshop.finance.service.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/advances")
public class AdvanceApplicationController {
    private final AdvanceApplicationService service;
    private final AuthService authService;
    public AdvanceApplicationController(AdvanceApplicationService service, AuthService authService) {
        this.service = service;
        this.authService = authService;
    }
    @GetMapping
    public List<AdvanceApplicationResponse> list(
            @RequestHeader("X-Auth-Token") String token,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) AdvanceType type,
            @RequestParam(required = false) AdvanceStatus status,
            @RequestParam(required = false) SettlementStatus settlementStatus
    ) {
        return service.list(authService.requireUser(token), keyword, type, status, settlementStatus);
    }
    @GetMapping("/{id}")
    public AdvanceApplicationResponse detail(@RequestHeader("X-Auth-Token") String token, @PathVariable Long id) {
        return service.detail(authService.requireUser(token), id);
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public AdvanceApplicationResponse create(
            @RequestHeader("X-Auth-Token") String token, @Valid @RequestBody AdvanceApplicationRequest request
    ) {
        return service.create(authService.requireUser(token), request);
    }
    @PutMapping("/{id}")
    public AdvanceApplicationResponse update(
            @RequestHeader("X-Auth-Token") String token, @PathVariable Long id,
            @Valid @RequestBody AdvanceApplicationRequest request
    ) {
        return service.update(authService.requireUser(token), id, request);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader("X-Auth-Token") String token, @PathVariable Long id) {
        service.delete(authService.requireUser(token), id);
    }
    @PostMapping("/{id}/submit")
    public AdvanceApplicationResponse submit(@RequestHeader("X-Auth-Token") String token, @PathVariable Long id) {
        return service.submit(authService.requireUser(token), id);
    }
    @PostMapping("/{id}/approval")
    public AdvanceApplicationResponse approve(
            @RequestHeader("X-Auth-Token") String token, @PathVariable Long id,
            @Valid @RequestBody ApprovalRequest request
    ) {
        return service.approve(authService.requireUser(token), id, request);
    }
    @PostMapping("/{id}/payment")
    public AdvanceApplicationResponse payment(
            @RequestHeader("X-Auth-Token") String token, @PathVariable Long id,
            @Valid @RequestBody PaymentRequest request
    ) {
        return service.confirmPayment(authService.requireUser(token), id, request);
    }
    @PostMapping("/{id}/offset")
    public AdvanceApplicationResponse offset(
            @RequestHeader("X-Auth-Token") String token, @PathVariable Long id,
            @Valid @RequestBody AdvanceOffsetRequest request
    ) {
        return service.offset(authService.requireUser(token), id, request);
    }
    @PostMapping("/{id}/attachments")
    public AdvanceAttachmentResponse upload(
            @RequestHeader("X-Auth-Token") String token, @PathVariable Long id,
            @RequestParam(defaultValue = "OTHER") AdvanceAttachmentType attachmentType,
            @RequestPart("file") MultipartFile file
    ) {
        return service.upload(authService.requireUser(token), id, attachmentType, file);
    }
}
