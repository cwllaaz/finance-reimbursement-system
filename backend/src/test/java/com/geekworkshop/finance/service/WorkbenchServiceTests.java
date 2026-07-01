package com.geekworkshop.finance.service;

import com.geekworkshop.finance.entity.*;
import com.geekworkshop.finance.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkbenchServiceTests {
    @Mock ReimbursementRepository reimbursementRepository;
    @Mock PurchaseApplicationRepository purchaseRepository;
    @Mock LaborApplicationRepository laborRepository;
    @Mock AdvanceApplicationRepository advanceRepository;
    @Mock ApprovalRecordRepository reimbursementApprovalRepository;
    @Mock PurchaseApprovalRecordRepository purchaseApprovalRepository;
    @Mock LaborApprovalRecordRepository laborApprovalRepository;
    @Mock AdvanceApprovalRecordRepository advanceApprovalRepository;

    private WorkbenchService service;
    private Department research;
    private Department finance;

    @BeforeEach
    void setUp() {
        service = new WorkbenchService(
                reimbursementRepository, purchaseRepository, laborRepository, advanceRepository,
                reimbursementApprovalRepository, purchaseApprovalRepository,
                laborApprovalRepository, advanceApprovalRepository
        );
        research = department(1L, "科研部");
        finance = department(2L, "财务部");
        lenient().when(reimbursementRepository.findAllForExport()).thenReturn(List.of());
        lenient().when(purchaseRepository.findAllDetails()).thenReturn(List.of());
        lenient().when(laborRepository.findAllDetails()).thenReturn(List.of());
        lenient().when(advanceRepository.findAllDetails()).thenReturn(List.of());
        lenient().when(reimbursementApprovalRepository.findDetailsByApproverId(anyLong())).thenReturn(List.of());
        lenient().when(purchaseApprovalRepository.findDetailsByApproverId(anyLong())).thenReturn(List.of());
        lenient().when(laborApprovalRepository.findDetailsByApproverId(anyLong())).thenReturn(List.of());
        lenient().when(advanceApprovalRepository.findDetailsByApproverId(anyLong())).thenReturn(List.of());
    }

    @Test
    void myApplicationsOnlyReturnsCurrentUsersApplications() {
        AppUser employee = user(10L, UserRole.EMPLOYEE, research);
        Reimbursement own = reimbursement(100L, employee, research, ReimbursementStatus.DRAFT);
        Reimbursement other = reimbursement(
                101L, user(11L, UserRole.EMPLOYEE, research), research, ReimbursementStatus.SUBMITTED);
        when(reimbursementRepository.findAllForExport()).thenReturn(List.of(own, other));

        var result = service.list(employee, WorkbenchScope.MY_APPLICATIONS, null, null, null);

        assertEquals(1, result.size());
        assertEquals(100L, result.getFirst().businessId());
        assertEquals("REIMBURSEMENT", result.getFirst().businessType());
    }

    @Test
    void officeMyApplicationsOnlyReturnsPurchases() {
        AppUser office = user(20L, UserRole.OFFICE, research);
        when(purchaseRepository.findAllDetails()).thenReturn(List.of(
                purchase(200L, office, research, PurchaseStatus.DRAFT)));

        var result = service.list(office, WorkbenchScope.MY_APPLICATIONS, null, null, null);

        assertEquals(1, result.size());
        assertEquals("PURCHASE", result.getFirst().businessType());
        verify(reimbursementRepository, never()).findAllForExport();
        verify(laborRepository, never()).findAllDetails();
        verify(advanceRepository, never()).findAllDetails();
    }

    @Test
    void managerTodoOnlyContainsCurrentNodeInOwnDepartment() {
        AppUser manager = user(30L, UserRole.DEPARTMENT_MANAGER, research);
        Reimbursement ownPending = reimbursement(
                100L, user(10L, UserRole.EMPLOYEE, research), research,
                ReimbursementStatus.FINANCE_INITIAL_APPROVED);
        Reimbursement otherDepartment = reimbursement(
                101L, user(11L, UserRole.EMPLOYEE, finance), finance,
                ReimbursementStatus.FINANCE_INITIAL_APPROVED);
        Reimbursement wrongNode = reimbursement(
                102L, user(12L, UserRole.EMPLOYEE, research), research,
                ReimbursementStatus.SUBMITTED);
        when(reimbursementRepository.findAllForExport())
                .thenReturn(List.of(ownPending, otherDepartment, wrongNode));

        var result = service.list(manager, WorkbenchScope.MY_TODOS, null, null, null);

        assertEquals(1, result.size());
        assertEquals(100L, result.getFirst().businessId());
    }

    @Test
    void doneOnlyReturnsRecordsHandledByCurrentUserAndExcludesSubmit() {
        AppUser financeUser = user(40L, UserRole.FINANCE, finance);
        Reimbursement reimbursement = reimbursement(
                100L, user(10L, UserRole.EMPLOYEE, research), research,
                ReimbursementStatus.FINANCE_INITIAL_APPROVED);
        ApprovalRecord handled = approvalRecord(reimbursement, financeUser, "FINANCE_INITIAL", 2);
        ApprovalRecord submitted = approvalRecord(reimbursement, financeUser, "SUBMIT", 1);
        when(reimbursementApprovalRepository.findDetailsByApproverId(40L))
                .thenReturn(List.of(handled, submitted));

        var result = service.list(financeUser, WorkbenchScope.DONE, null, null, null);

        assertEquals(1, result.size());
        assertEquals(100L, result.getFirst().businessId());
        assertEquals(handled.getCreatedAt(), result.getFirst().time());
    }

    @Test
    void filtersByBusinessTypeStatusAndKeyword() {
        AppUser employee = user(10L, UserRole.EMPLOYEE, research);
        Reimbursement reimbursement = reimbursement(100L, employee, research, ReimbursementStatus.DRAFT);
        reimbursement.setTitle("差旅交通报销");
        PurchaseApplication purchase = purchase(200L, employee, research, PurchaseStatus.DRAFT);
        when(reimbursementRepository.findAllForExport()).thenReturn(List.of(reimbursement));
        when(purchaseRepository.findAllDetails()).thenReturn(List.of(purchase));

        var result = service.list(
                employee, WorkbenchScope.MY_APPLICATIONS, "REIMBURSEMENT", "DRAFT", "交通");

        assertEquals(1, result.size());
        assertEquals("差旅交通报销", result.getFirst().title());
        assertTrue(result.stream().allMatch(item -> "DRAFT".equals(item.status())));
    }

    private ApprovalRecord approvalRecord(
            Reimbursement reimbursement, AppUser approver, String node, int minutes
    ) {
        ApprovalRecord record = new ApprovalRecord();
        ReflectionTestUtils.setField(record, "id", (long) minutes);
        ReflectionTestUtils.setField(record, "createdAt", LocalDateTime.now().plusMinutes(minutes));
        record.setReimbursement(reimbursement);
        record.setApprover(approver);
        record.setApprovalNode(node);
        record.setAction(ApprovalAction.APPROVE);
        return record;
    }

    private Reimbursement reimbursement(
            Long id, AppUser applicant, Department department, ReimbursementStatus status
    ) {
        Reimbursement value = new Reimbursement();
        base(value, id);
        value.setApprovalNumber("BX2026070100" + id % 10);
        value.setTitle("测试报销");
        value.setApplicant(applicant);
        value.setDepartment(department);
        value.setAmount(new BigDecimal("100.00"));
        value.setStatus(status);
        return value;
    }

    private PurchaseApplication purchase(
            Long id, AppUser applicant, Department department, PurchaseStatus status
    ) {
        PurchaseApplication value = new PurchaseApplication();
        base(value, id);
        value.setApplicationNumber("CG2026070100" + id % 10);
        value.setPurchaseReason("设备采购");
        value.setPurchaseMethod("询价");
        value.setApplicant(applicant);
        value.setDepartment(department);
        value.setAmount(new BigDecimal("500.00"));
        value.setStatus(status);
        return value;
    }

    private void base(Object value, Long id) {
        ReflectionTestUtils.setField(value, "id", id);
        ReflectionTestUtils.setField(value, "createdAt", LocalDateTime.now().minusMinutes(id));
        ReflectionTestUtils.setField(value, "updatedAt", LocalDateTime.now());
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

    private Department department(Long id, String name) {
        Department department = new Department();
        ReflectionTestUtils.setField(department, "id", id);
        department.setCode("D" + id);
        department.setName(name);
        return department;
    }
}
