package com.geekworkshop.finance.controller;

import com.geekworkshop.finance.dto.*;
import com.geekworkshop.finance.entity.*;
import com.geekworkshop.finance.service.AuthService;
import com.geekworkshop.finance.service.PurchaseApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseApplicationController {
    private final PurchaseApplicationService purchaseService;
    private final AuthService authService;

    public PurchaseApplicationController(PurchaseApplicationService purchaseService, AuthService authService) {
        this.purchaseService = purchaseService;
        this.authService = authService;
    }

    @GetMapping
    public List<PurchaseApplicationResponse> list(
            @RequestHeader("X-Auth-Token") String token,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) PurchaseStatus status
    ) {
        return purchaseService.list(authService.requireUser(token), keyword, status);
    }

    @GetMapping("/pending")
    public List<PurchaseApplicationResponse> pending(@RequestHeader("X-Auth-Token") String token) {
        return purchaseService.pending(authService.requireUser(token));
    }

    @GetMapping("/{id}")
    public PurchaseApplicationResponse detail(
            @RequestHeader("X-Auth-Token") String token, @PathVariable Long id
    ) {
        return purchaseService.detail(authService.requireUser(token), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseApplicationResponse create(
            @RequestHeader("X-Auth-Token") String token,
            @Valid @RequestBody PurchaseApplicationRequest request
    ) {
        return purchaseService.create(authService.requireUser(token), request);
    }

    @PutMapping("/{id}")
    public PurchaseApplicationResponse update(
            @RequestHeader("X-Auth-Token") String token, @PathVariable Long id,
            @Valid @RequestBody PurchaseApplicationRequest request
    ) {
        return purchaseService.update(authService.requireUser(token), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader("X-Auth-Token") String token, @PathVariable Long id) {
        purchaseService.delete(authService.requireUser(token), id);
    }

    @PostMapping("/{id}/submit")
    public PurchaseApplicationResponse submit(
            @RequestHeader("X-Auth-Token") String token, @PathVariable Long id
    ) {
        return purchaseService.submit(authService.requireUser(token), id);
    }

    @PostMapping("/{id}/approval")
    public PurchaseApplicationResponse approve(
            @RequestHeader("X-Auth-Token") String token, @PathVariable Long id,
            @Valid @RequestBody ApprovalRequest request
    ) {
        return purchaseService.approve(authService.requireUser(token), id, request);
    }

    @PostMapping("/{id}/attachments")
    public PurchaseAttachmentResponse upload(
            @RequestHeader("X-Auth-Token") String token, @PathVariable Long id,
            @RequestParam(defaultValue = "OTHER") PurchaseAttachmentType attachmentType,
            @RequestPart("file") MultipartFile file
    ) {
        return purchaseService.upload(authService.requireUser(token), id, attachmentType, file);
    }
}
