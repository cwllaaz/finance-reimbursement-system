package com.geekworkshop.finance.service;

import com.geekworkshop.finance.dto.ApprovalRequest;
import com.geekworkshop.finance.dto.PurchaseApplicationRequest;
import com.geekworkshop.finance.dto.PurchaseItemRequest;
import com.geekworkshop.finance.entity.*;
import com.geekworkshop.finance.exception.BusinessException;
import com.geekworkshop.finance.exception.ForbiddenException;
import com.geekworkshop.finance.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseApplicationServiceTests {
    @Mock PurchaseApplicationRepository applicationRepository;
    @Mock PurchaseAttachmentRepository attachmentRepository;
    @Mock PurchaseApprovalRecordRepository approvalRepository;
    @Mock OperationLogService operationLogService;

    private PurchaseApplicationService service;

    @BeforeEach
    void setUp() {
        service = new PurchaseApplicationService(
                applicationRepository, attachmentRepository, approvalRepository, operationLogService);
        lenient().when(attachmentRepository.findByPurchaseApplicationIdOrderByCreatedAtAsc(any())).thenReturn(List.of());
        lenient().when(approvalRepository.findByPurchaseApplicationIdOrderByCreatedAtAsc(any())).thenReturn(List.of());
        lenient().when(applicationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void managerCanOnlyViewOwnDepartmentPurchases() {
        Department ownDepartment = department(1L);
        Department otherDepartment = department(2L);
        AppUser manager = user(10L, UserRole.DEPARTMENT_MANAGER, ownDepartment);
        PurchaseApplication own = application(100L, ownDepartment, PurchaseStatus.FINANCE_APPROVED, "1000");
        PurchaseApplication other = application(101L, otherDepartment, PurchaseStatus.FINANCE_APPROVED, "2000");
        when(applicationRepository.findAllDetails()).thenReturn(List.of(own, other));

        var result = service.list(manager, null, null);

        assertEquals(1, result.size());
        assertEquals(100L, result.getFirst().id());
    }

    @Test
    void officeCanCreatePurchaseApplication() {
        Department department = department(1L);
        AppUser office = user(60L, UserRole.OFFICE, department);
        PurchaseItemRequest item = new PurchaseItemRequest();
        item.setItemName("办公设备");
        item.setUnitPrice(new BigDecimal("1200.00"));
        item.setQuantity(1);
        PurchaseApplicationRequest request = new PurchaseApplicationRequest();
        request.setPurchaseMethod("询价采购");
        request.setTaxExempt(false);
        request.setPurchaseReason("办公室设备购置");
        request.setItems(List.of(item));
        when(applicationRepository.findTopByApplicationNumberStartingWithOrderByApplicationNumberDesc(any()))
                .thenReturn(Optional.empty());

        var result = service.create(office, request);

        assertEquals(office.getId(), result.applicantId());
        assertEquals(PurchaseStatus.DRAFT, result.status());
    }

    @Test
    void financeApprovalMovesPurchaseToDepartmentStage() {
        Department department = department(1L);
        AppUser finance = user(20L, UserRole.FINANCE, department);
        PurchaseApplication application = application(100L, department, PurchaseStatus.SUBMITTED, "1000");
        when(applicationRepository.findDetailById(100L)).thenReturn(Optional.of(application));
        ApprovalRequest request = approval(ApprovalAction.APPROVE, "预算审核通过");

        var response = service.approve(finance, 100L, request);

        assertEquals(PurchaseStatus.FINANCE_APPROVED, response.status());
        verify(approvalRepository).save(any(PurchaseApprovalRecord.class));
    }

    @Test
    void managerFromOtherDepartmentCannotApprove() {
        Department ownDepartment = department(1L);
        Department otherDepartment = department(2L);
        AppUser manager = user(30L, UserRole.DEPARTMENT_MANAGER, otherDepartment);
        PurchaseApplication application = application(100L, ownDepartment, PurchaseStatus.FINANCE_APPROVED, "1000");
        when(applicationRepository.findDetailById(100L)).thenReturn(Optional.of(application));

        assertThrows(ForbiddenException.class,
                () -> service.approve(manager, 100L, approval(ApprovalAction.APPROVE, "同意")));
    }

    @Test
    void largePurchaseRequiresMeetingMinutesBeforeSubmission() {
        Department department = department(1L);
        AppUser applicant = user(40L, UserRole.OFFICE, department);
        PurchaseApplication application = application(100L, department, PurchaseStatus.DRAFT, "50000.01");
        application.setApplicant(applicant);
        when(applicationRepository.findDetailById(100L)).thenReturn(Optional.of(application));
        when(attachmentRepository.existsByPurchaseApplicationIdAndAttachmentType(
                100L, PurchaseAttachmentType.MEETING_MINUTES)).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.submit(applicant, 100L));

        assertEquals("5万元以上申购必须上传院务委员会审议材料", exception.getMessage());
    }

    @Test
    void purchaseBelowThresholdCanBeSubmittedWithoutMeetingMinutes() {
        Department department = department(1L);
        AppUser applicant = user(41L, UserRole.OFFICE, department);
        PurchaseApplication application = application(101L, department, PurchaseStatus.DRAFT, "49999.99");
        application.setApplicant(applicant);
        when(applicationRepository.findDetailById(101L)).thenReturn(Optional.of(application));

        var response = service.submit(applicant, 101L);

        assertEquals(PurchaseStatus.SUBMITTED, response.status());
        verify(attachmentRepository, never()).existsByPurchaseApplicationIdAndAttachmentType(
                101L, PurchaseAttachmentType.MEETING_MINUTES);
    }

    @Test
    void purchaseAtThresholdCanBeSubmittedWithoutMeetingMinutes() {
        Department department = department(1L);
        AppUser applicant = user(42L, UserRole.OFFICE, department);
        PurchaseApplication application = application(102L, department, PurchaseStatus.DRAFT, "50000.00");
        application.setApplicant(applicant);
        when(applicationRepository.findDetailById(102L)).thenReturn(Optional.of(application));

        var response = service.submit(applicant, 102L);

        assertEquals(PurchaseStatus.SUBMITTED, response.status());
        verify(attachmentRepository, never()).existsByPurchaseApplicationIdAndAttachmentType(
                102L, PurchaseAttachmentType.MEETING_MINUTES);
    }

    @Test
    void executiveApprovalCompletesPurchase() {
        Department department = department(1L);
        AppUser executive = user(50L, UserRole.EXECUTIVE, department);
        PurchaseApplication application = application(100L, department, PurchaseStatus.DEPARTMENT_APPROVED, "1200");
        when(applicationRepository.findDetailById(100L)).thenReturn(Optional.of(application));

        var response = service.approve(executive, 100L, approval(ApprovalAction.APPROVE, "同意采购"));

        assertEquals(PurchaseStatus.COMPLETED, response.status());
    }

    private ApprovalRequest approval(ApprovalAction action, String comment) {
        ApprovalRequest request = new ApprovalRequest();
        request.setAction(action);
        request.setComment(comment);
        return request;
    }

    private PurchaseApplication application(
            Long id, Department department, PurchaseStatus status, String amount
    ) {
        PurchaseApplication application = new PurchaseApplication();
        ReflectionTestUtils.setField(application, "id", id);
        application.setApplicationNumber("CG20260630001");
        application.setApplicant(user(99L, UserRole.EMPLOYEE, department));
        application.setDepartment(department);
        application.setAmount(new BigDecimal(amount));
        application.setPurchaseMethod("询价采购");
        application.setPurchaseReason("科研设备购置");
        application.setStatus(status);
        PurchaseItem item = new PurchaseItem();
        item.setPurchaseApplication(application);
        item.setItemName("测试设备");
        item.setUnitPrice(new BigDecimal(amount));
        item.setQuantity(1);
        item.setTotalPrice(new BigDecimal(amount));
        application.getItems().add(item);
        return application;
    }

    private AppUser user(Long id, UserRole role, Department department) {
        AppUser user = new AppUser();
        ReflectionTestUtils.setField(user, "id", id);
        user.setUsername(role.name().toLowerCase());
        user.setRealName(role.name());
        user.setRole(role);
        user.setDepartment(department);
        return user;
    }

    private Department department(Long id) {
        Department department = new Department();
        ReflectionTestUtils.setField(department, "id", id);
        department.setName("部门" + id);
        department.setCode("D" + id);
        return department;
    }
}
