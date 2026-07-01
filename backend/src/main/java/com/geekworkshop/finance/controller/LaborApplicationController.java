package com.geekworkshop.finance.controller;

import com.geekworkshop.finance.dto.*;
import com.geekworkshop.finance.entity.*;
import com.geekworkshop.finance.service.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/labor-applications")
public class LaborApplicationController {
    private final LaborApplicationService laborService;
    private final AuthService authService;

    public LaborApplicationController(LaborApplicationService laborService, AuthService authService) {
        this.laborService = laborService;
        this.authService = authService;
    }

    @GetMapping
    public List<LaborApplicationResponse> list(
            @RequestHeader("X-Auth-Token") String token,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) LaborStatus status
    ) {
        return laborService.list(authService.requireUser(token), keyword, status);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestHeader("X-Auth-Token") String token) {
        byte[] content = laborService.exportExcel(authService.requireUser(token));
        String filename = URLEncoder.encode("劳务酬金发放-" + LocalDate.now() + ".xlsx", StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    @GetMapping("/{id}")
    public LaborApplicationResponse detail(
            @RequestHeader("X-Auth-Token") String token, @PathVariable Long id
    ) {
        return laborService.detail(authService.requireUser(token), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LaborApplicationResponse create(
            @RequestHeader("X-Auth-Token") String token, @Valid @RequestBody LaborApplicationRequest request
    ) {
        return laborService.create(authService.requireUser(token), request);
    }

    @PutMapping("/{id}")
    public LaborApplicationResponse update(
            @RequestHeader("X-Auth-Token") String token, @PathVariable Long id,
            @Valid @RequestBody LaborApplicationRequest request
    ) {
        return laborService.update(authService.requireUser(token), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader("X-Auth-Token") String token, @PathVariable Long id) {
        laborService.delete(authService.requireUser(token), id);
    }

    @PostMapping("/{id}/submit")
    public LaborApplicationResponse submit(
            @RequestHeader("X-Auth-Token") String token, @PathVariable Long id
    ) {
        return laborService.submit(authService.requireUser(token), id);
    }

    @PostMapping("/{id}/approval")
    public LaborApplicationResponse approve(
            @RequestHeader("X-Auth-Token") String token, @PathVariable Long id,
            @Valid @RequestBody ApprovalRequest request
    ) {
        return laborService.approve(authService.requireUser(token), id, request);
    }

    @PostMapping("/{id}/payment")
    public LaborApplicationResponse payment(
            @RequestHeader("X-Auth-Token") String token, @PathVariable Long id,
            @Valid @RequestBody PaymentRequest request
    ) {
        return laborService.confirmPayment(authService.requireUser(token), id, request);
    }

    @PostMapping("/{id}/attachments")
    public LaborAttachmentResponse upload(
            @RequestHeader("X-Auth-Token") String token, @PathVariable Long id,
            @RequestParam(defaultValue = "OTHER") LaborAttachmentType attachmentType,
            @RequestPart("file") MultipartFile file
    ) {
        return laborService.upload(authService.requireUser(token), id, attachmentType, file);
    }
}
