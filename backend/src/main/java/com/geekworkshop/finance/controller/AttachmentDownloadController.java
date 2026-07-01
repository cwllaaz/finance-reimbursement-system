package com.geekworkshop.finance.controller;

import com.geekworkshop.finance.entity.AppUser;
import com.geekworkshop.finance.entity.AttachmentModule;
import com.geekworkshop.finance.service.AttachmentDownloadService;
import com.geekworkshop.finance.service.AuthService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/attachments")
public class AttachmentDownloadController {
    private final AttachmentDownloadService downloadService;
    private final AuthService authService;

    public AttachmentDownloadController(AttachmentDownloadService downloadService, AuthService authService) {
        this.downloadService = downloadService;
        this.authService = authService;
    }

    @GetMapping("/{module}/{id}/download")
    public ResponseEntity<FileSystemResource> download(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable AttachmentModule module,
            @PathVariable Long id
    ) {
        AppUser user = authService.requireUser(token);
        AttachmentDownloadService.DownloadFile file = downloadService.load(user, module, id);
        MediaType contentType;
        try {
            contentType = MediaType.parseMediaType(file.contentType());
        } catch (Exception ignored) {
            contentType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.fileName(), StandardCharsets.UTF_8)
                        .build().toString())
                .body(new FileSystemResource(file.path()));
    }
}
