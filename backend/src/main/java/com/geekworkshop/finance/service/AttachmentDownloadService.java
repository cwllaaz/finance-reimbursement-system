package com.geekworkshop.finance.service;

import com.geekworkshop.finance.entity.*;
import com.geekworkshop.finance.exception.BusinessException;
import com.geekworkshop.finance.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class AttachmentDownloadService {
    private final AttachmentRepository reimbursementAttachmentRepository;
    private final PurchaseAttachmentRepository purchaseAttachmentRepository;
    private final LaborAttachmentRepository laborAttachmentRepository;
    private final AdvanceAttachmentRepository advanceAttachmentRepository;
    private final IncomeAttachmentRepository incomeAttachmentRepository;
    private final ReimbursementService reimbursementService;
    private final PurchaseApplicationService purchaseService;
    private final LaborApplicationService laborService;
    private final AdvanceApplicationService advanceService;
    private final IncomeLedgerService incomeService;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    public AttachmentDownloadService(
            AttachmentRepository reimbursementAttachmentRepository,
            PurchaseAttachmentRepository purchaseAttachmentRepository,
            LaborAttachmentRepository laborAttachmentRepository,
            AdvanceAttachmentRepository advanceAttachmentRepository,
            IncomeAttachmentRepository incomeAttachmentRepository,
            ReimbursementService reimbursementService,
            PurchaseApplicationService purchaseService,
            LaborApplicationService laborService,
            AdvanceApplicationService advanceService,
            IncomeLedgerService incomeService
    ) {
        this.reimbursementAttachmentRepository = reimbursementAttachmentRepository;
        this.purchaseAttachmentRepository = purchaseAttachmentRepository;
        this.laborAttachmentRepository = laborAttachmentRepository;
        this.advanceAttachmentRepository = advanceAttachmentRepository;
        this.incomeAttachmentRepository = incomeAttachmentRepository;
        this.reimbursementService = reimbursementService;
        this.purchaseService = purchaseService;
        this.laborService = laborService;
        this.advanceService = advanceService;
        this.incomeService = incomeService;
    }

    @Transactional(readOnly = true)
    public DownloadFile load(AppUser user, AttachmentModule module, Long attachmentId) {
        return switch (module) {
            case REIMBURSEMENT -> {
                Attachment attachment = reimbursementAttachmentRepository.findById(attachmentId)
                        .orElseThrow(() -> new BusinessException("附件不存在"));
                reimbursementService.detail(user, attachment.getReimbursement().getId());
                yield file(attachment.getFileUrl(), attachment.getFileName(), attachment.getFileType());
            }
            case PURCHASE -> {
                PurchaseAttachment attachment = purchaseAttachmentRepository.findById(attachmentId)
                        .orElseThrow(() -> new BusinessException("附件不存在"));
                purchaseService.detail(user, attachment.getPurchaseApplication().getId());
                yield file(attachment.getFileUrl(), attachment.getFileName(), attachment.getFileType());
            }
            case LABOR -> {
                LaborAttachment attachment = laborAttachmentRepository.findById(attachmentId)
                        .orElseThrow(() -> new BusinessException("附件不存在"));
                laborService.detail(user, attachment.getLaborApplication().getId());
                yield file(attachment.getFileUrl(), attachment.getFileName(), attachment.getFileType());
            }
            case ADVANCE -> {
                AdvanceAttachment attachment = advanceAttachmentRepository.findById(attachmentId)
                        .orElseThrow(() -> new BusinessException("附件不存在"));
                advanceService.detail(user, attachment.getAdvanceApplication().getId());
                yield file(attachment.getFileUrl(), attachment.getFileName(), attachment.getFileType());
            }
            case INCOME -> {
                IncomeAttachment attachment = incomeAttachmentRepository.findById(attachmentId)
                        .orElseThrow(() -> new BusinessException("附件不存在"));
                incomeService.detail(user, attachment.getIncomeRecord().getId());
                yield file(attachment.getFileUrl(), attachment.getFileName(), attachment.getFileType());
            }
        };
    }

    private DownloadFile file(String fileUrl, String fileName, String contentType) {
        String relative = fileUrl.startsWith("/uploads/")
                ? fileUrl.substring("/uploads/".length())
                : fileUrl.replaceFirst("^/+", "");
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path path = root.resolve(relative).normalize();
        if (!path.startsWith(root) || !Files.isRegularFile(path)) {
            throw new BusinessException("附件文件不存在或路径不合法");
        }
        return new DownloadFile(path, fileName, contentType);
    }

    public record DownloadFile(Path path, String fileName, String contentType) {
    }
}
