package com.geekworkshop.finance.controller;

import com.geekworkshop.finance.entity.AppUser;
import com.geekworkshop.finance.entity.DocumentModule;
import com.geekworkshop.finance.service.AuthService;
import com.geekworkshop.finance.service.CompleteDocumentPdfService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/documents")
public class CompleteDocumentController {
    private final CompleteDocumentPdfService pdfService;
    private final AuthService authService;

    public CompleteDocumentController(CompleteDocumentPdfService pdfService, AuthService authService) {
        this.pdfService = pdfService;
        this.authService = authService;
    }

    @GetMapping("/{module}/{id}/pdf")
    public ResponseEntity<byte[]> download(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable DocumentModule module,
            @PathVariable Long id
    ) {
        AppUser user = authService.requireUser(token);
        CompleteDocumentPdfService.PdfFile file = pdfService.generate(user, module, id);
        String encoded = URLEncoder.encode(file.fileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .body(file.bytes());
    }
}
