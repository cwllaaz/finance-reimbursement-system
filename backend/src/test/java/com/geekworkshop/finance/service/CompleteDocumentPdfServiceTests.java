package com.geekworkshop.finance.service;

import com.geekworkshop.finance.dto.PurchaseApplicationResponse;
import com.geekworkshop.finance.entity.*;
import com.geekworkshop.finance.exception.ForbiddenException;
import com.lowagie.text.pdf.PdfReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompleteDocumentPdfServiceTests {
    @Mock ReimbursementService reimbursementService;
    @Mock PurchaseApplicationService purchaseService;
    @Mock LaborApplicationService laborService;
    @Mock AdvanceApplicationService advanceService;
    @Mock OperationLogService operationLogService;
    private CompleteDocumentPdfService service;
    private Department research;

    @BeforeEach
    void setUp() {
        service = new CompleteDocumentPdfService(
                reimbursementService, purchaseService, laborService, advanceService, operationLogService);
        research = department(1L, "科研管理部");
    }

    @Test
    void committeeCanGenerateChinesePrintablePdf() throws Exception {
        when(purchaseService.detail(any(), eq(10L))).thenReturn(purchase(8L));

        var result = service.generate(user(20L, UserRole.COMMITTEE, department(2L, "院领导")), DocumentModule.PURCHASE, 10L);

        assertTrue(result.bytes().length > 500);
        assertEquals("%PDF", new String(result.bytes(), 0, 4));
        PdfReader reader = new PdfReader(result.bytes());
        assertEquals(1, reader.getNumberOfPages());
        assertTrue(reader.getInfo().get("Title").contains("CG20260703001"));
        reader.close();
        Path sample = Path.of("target", "test-output", "complete-document-sample.pdf");
        Files.createDirectories(sample.getParent());
        Files.write(sample, result.bytes());
        verify(operationLogService).record(any(), eq("单据归档"), eq("下载完整单据"),
                eq(10L), eq("CG20260703001"), anyString());
    }

    @Test
    void employeeCanOnlyDownloadOwnDocumentAndOfficeIsDenied() {
        when(purchaseService.detail(any(), eq(10L))).thenReturn(purchase(8L));

        assertDoesNotThrow(() -> service.generate(
                user(8L, UserRole.EMPLOYEE, research), DocumentModule.PURCHASE, 10L));
        assertThrows(ForbiddenException.class, () -> service.generate(
                user(9L, UserRole.EMPLOYEE, research), DocumentModule.PURCHASE, 10L));
        assertThrows(ForbiddenException.class, () -> service.generate(
                user(30L, UserRole.OFFICE, research), DocumentModule.PURCHASE, 10L));
    }

    private PurchaseApplicationResponse purchase(Long applicantId) {
        LocalDateTime now = LocalDateTime.now();
        return new PurchaseApplicationResponse(
                10L, "CG20260703001", applicantId, "张同学", 1L, "科研管理部",
                "13800000000", new BigDecimal("1280.00"), "YS-001", "询价采购",
                false, "科研楼", "购买显示器", null, PurchaseStatus.COMPLETED,
                now, now, now, List.of(), List.of(), List.of()
        );
    }

    private AppUser user(Long id, UserRole role, Department department) {
        AppUser user = new AppUser();
        ReflectionTestUtils.setField(user, "id", id);
        user.setRole(role);
        user.setDepartment(department);
        return user;
    }

    private Department department(Long id, String name) {
        Department department = new Department();
        ReflectionTestUtils.setField(department, "id", id);
        department.setName(name);
        department.setCode("D" + id);
        return department;
    }
}
