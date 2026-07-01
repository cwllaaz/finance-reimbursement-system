package com.geekworkshop.finance.service;

import com.geekworkshop.finance.entity.*;
import com.geekworkshop.finance.exception.ForbiddenException;
import com.geekworkshop.finance.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttachmentDownloadServiceTests {
    @Mock AttachmentRepository reimbursementAttachmentRepository;
    @Mock PurchaseAttachmentRepository purchaseAttachmentRepository;
    @Mock LaborAttachmentRepository laborAttachmentRepository;
    @Mock AdvanceAttachmentRepository advanceAttachmentRepository;
    @Mock IncomeAttachmentRepository incomeAttachmentRepository;
    @Mock ReimbursementService reimbursementService;
    @Mock PurchaseApplicationService purchaseService;
    @Mock LaborApplicationService laborService;
    @Mock AdvanceApplicationService advanceService;
    @Mock IncomeLedgerService incomeService;
    @TempDir Path uploadDir;

    private AttachmentDownloadService service;
    private AppUser user;

    @BeforeEach
    void setUp() {
        service = new AttachmentDownloadService(
                reimbursementAttachmentRepository, purchaseAttachmentRepository,
                laborAttachmentRepository, advanceAttachmentRepository, incomeAttachmentRepository,
                reimbursementService, purchaseService, laborService, advanceService, incomeService
        );
        ReflectionTestUtils.setField(service, "uploadDir", uploadDir.toString());
        user = new AppUser();
        ReflectionTestUtils.setField(user, "id", 1L);
        user.setRole(UserRole.EMPLOYEE);
    }

    @Test
    void authorizedUserCanDownloadAttachment() throws Exception {
        Path invoiceDir = Files.createDirectories(uploadDir.resolve("invoice"));
        Path file = invoiceDir.resolve("stored.pdf");
        Files.write(file, new byte[]{1, 2, 3});
        Attachment attachment = attachment("/uploads/invoice/stored.pdf");
        when(reimbursementAttachmentRepository.findById(10L)).thenReturn(Optional.of(attachment));

        var result = service.load(user, AttachmentModule.REIMBURSEMENT, 10L);

        assertEquals(file, result.path());
        assertEquals("invoice.pdf", result.fileName());
        verify(reimbursementService).detail(user, 99L);
    }

    @Test
    void businessPermissionFailureBlocksDownload() {
        Attachment attachment = attachment("/uploads/invoice/stored.pdf");
        when(reimbursementAttachmentRepository.findById(10L)).thenReturn(Optional.of(attachment));
        doThrow(new ForbiddenException("无权查看"))
                .when(reimbursementService).detail(user, 99L);

        assertThrows(ForbiddenException.class,
                () -> service.load(user, AttachmentModule.REIMBURSEMENT, 10L));
    }

    @Test
    void attachmentPathCannotEscapeUploadDirectory() {
        Attachment attachment = attachment("/uploads/../../secret.pdf");
        when(reimbursementAttachmentRepository.findById(10L)).thenReturn(Optional.of(attachment));

        assertThrows(RuntimeException.class,
                () -> service.load(user, AttachmentModule.REIMBURSEMENT, 10L));
    }

    private Attachment attachment(String fileUrl) {
        Reimbursement reimbursement = new Reimbursement();
        ReflectionTestUtils.setField(reimbursement, "id", 99L);
        Attachment attachment = new Attachment();
        ReflectionTestUtils.setField(attachment, "id", 10L);
        attachment.setReimbursement(reimbursement);
        attachment.setFileName("invoice.pdf");
        attachment.setFileUrl(fileUrl);
        attachment.setFileType("application/pdf");
        attachment.setFileSize(3L);
        attachment.setAttachmentType(AttachmentType.INVOICE);
        return attachment;
    }
}
