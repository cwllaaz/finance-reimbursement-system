package com.geekworkshop.finance.service;

import com.geekworkshop.finance.exception.BusinessException;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class SecureFileSupport {
    public static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    private static final Map<String, Set<String>> ALLOWED_TYPES = Map.of(
            "jpg", Set.of("image/jpeg", "image/jpg"),
            "jpeg", Set.of("image/jpeg", "image/jpg"),
            "png", Set.of("image/png"),
            "pdf", Set.of("application/pdf"),
            "doc", Set.of("application/msword"),
            "docx", Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            "xls", Set.of("application/vnd.ms-excel"),
            "xlsx", Set.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    );

    private SecureFileSupport() {
    }

    public static String validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("附件大小不能超过 10MB");
        }
        String originalName = Optional.ofNullable(file.getOriginalFilename()).orElse("").trim();
        if (originalName.isEmpty() || originalName.indexOf('\0') >= 0
                || originalName.contains("/") || originalName.contains("\\")) {
            throw new BusinessException("附件文件名不合法");
        }
        String safeName = Paths.get(originalName).getFileName().toString();
        int dot = safeName.lastIndexOf('.');
        if (dot <= 0 || dot == safeName.length() - 1) {
            throw new BusinessException("附件必须包含允许的文件扩展名");
        }
        String extension = safeName.substring(dot + 1).toLowerCase(Locale.ROOT);
        Set<String> allowedMimes = ALLOWED_TYPES.get(extension);
        if (allowedMimes == null) {
            throw new BusinessException("不允许上传该类型文件");
        }
        String mimeType = Optional.ofNullable(file.getContentType())
                .orElse("")
                .toLowerCase(Locale.ROOT);
        if (!allowedMimes.contains(mimeType)) {
            throw new BusinessException("文件扩展名与 MIME 类型不匹配");
        }
        return safeName;
    }
}
