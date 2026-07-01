package com.geekworkshop.finance.service;

import com.geekworkshop.finance.dto.DashboardStatsResponse;
import com.geekworkshop.finance.dto.ApprovalRequest;
import com.geekworkshop.finance.dto.PaymentRequest;
import com.geekworkshop.finance.dto.ReimbursementRequest;
import com.geekworkshop.finance.dto.ReimbursementResponse;
import com.geekworkshop.finance.entity.AppUser;
import com.geekworkshop.finance.entity.ApprovalAction;
import com.geekworkshop.finance.entity.AttachmentType;
import com.geekworkshop.finance.entity.Budget;
import com.geekworkshop.finance.entity.Department;
import com.geekworkshop.finance.entity.Reimbursement;
import com.geekworkshop.finance.entity.ReimbursementStatus;
import com.geekworkshop.finance.entity.UserRole;
import com.geekworkshop.finance.exception.ForbiddenException;
import com.geekworkshop.finance.exception.BusinessException;
import com.geekworkshop.finance.repository.ApprovalRecordRepository;
import com.geekworkshop.finance.repository.AppUserRepository;
import com.geekworkshop.finance.repository.AttachmentRepository;
import com.geekworkshop.finance.repository.BudgetRepository;
import com.geekworkshop.finance.repository.DepartmentRepository;
import com.geekworkshop.finance.repository.InvoiceOcrResultRepository;
import com.geekworkshop.finance.repository.ReimbursementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReimbursementServiceRolePermissionTests {

    @Mock
    private ReimbursementRepository reimbursementRepository;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private ApprovalRecordRepository approvalRecordRepository;
    @Mock
    private BudgetRepository budgetRepository;
    @Mock
    private AttachmentRepository attachmentRepository;
    @Mock
    private InvoiceOcrResultRepository invoiceOcrResultRepository;
    @Mock
    private BaiduOcrService baiduOcrService;
    @Mock
    private OperationLogService operationLogService;

    private ReimbursementService service;

    @BeforeEach
    void setUp() {
        service = new ReimbursementService(
                reimbursementRepository,
                appUserRepository,
                departmentRepository,
                approvalRecordRepository,
                budgetRepository,
                attachmentRepository,
                invoiceOcrResultRepository,
                baiduOcrService,
                operationLogService
        );
    }

    @Test
    void employeeOfficeAndCashierCannotAccessDashboard() {
        Department department = department(1L, "Research");

        assertThrows(ForbiddenException.class,
                () -> service.dashboardStats(user(UserRole.EMPLOYEE, department)));
        assertThrows(ForbiddenException.class,
                () -> service.dashboardStats(user(UserRole.OFFICE, department)));
        assertThrows(ForbiddenException.class,
                () -> service.dashboardStats(user(UserRole.CASHIER, department)));
    }

    @Test
    void managerDashboardOnlyContainsOwnDepartment() {
        Department research = department(1L, "Research");
        Department finance = department(2L, "Finance");
        AppUser manager = user(UserRole.DEPARTMENT_MANAGER, research);
        Reimbursement own = reimbursement(research, "100.00", ReimbursementStatus.APPROVED);
        Reimbursement other = reimbursement(finance, "900.00", ReimbursementStatus.APPROVED);
        when(reimbursementRepository.search(null, null)).thenReturn(List.of(own, other));
        when(budgetRepository.findAllWithDepartment()).thenReturn(List.of(
                budget(research, "1000.00"),
                budget(finance, "5000.00")
        ));

        DashboardStatsResponse result = service.dashboardStats(manager);

        assertEquals(1L, result.getReimbursementCount());
        assertEquals(new BigDecimal("100.00"), result.getMonthAmount());
        assertEquals(1, result.getBudgets().size());
        assertEquals(1L, result.getBudgets().getFirst().getDepartmentId());
    }

    @Test
    void executiveDashboardContainsAllDepartments() {
        Department research = department(1L, "Research");
        Department finance = department(2L, "Finance");
        AppUser executive = user(UserRole.EXECUTIVE, department(3L, "Leadership"));
        when(reimbursementRepository.search(null, null)).thenReturn(List.of(
                reimbursement(research, "100.00", ReimbursementStatus.APPROVED),
                reimbursement(finance, "900.00", ReimbursementStatus.SUBMITTED)
        ));
        when(budgetRepository.findAllWithDepartment()).thenReturn(List.of(
                budget(research, "1000.00"),
                budget(finance, "5000.00")
        ));

        DashboardStatsResponse result = service.dashboardStats(executive);

        assertEquals(2L, result.getReimbursementCount());
        assertEquals(new BigDecimal("1000.00"), result.getMonthAmount());
        assertEquals(2, result.getBudgets().size());
    }

    @Test
    void financeAndAdminDashboardContainAllDepartments() {
        Department research = department(1L, "Research");
        Department finance = department(2L, "Finance");
        when(reimbursementRepository.search(null, null)).thenReturn(List.of(
                reimbursement(research, "100.00", ReimbursementStatus.APPROVED),
                reimbursement(finance, "900.00", ReimbursementStatus.SUBMITTED)
        ));
        when(budgetRepository.findAllWithDepartment()).thenReturn(List.of(
                budget(research, "1000.00"),
                budget(finance, "5000.00")
        ));

        DashboardStatsResponse financeResult = service.dashboardStats(user(UserRole.FINANCE, finance));
        DashboardStatsResponse adminResult = service.dashboardStats(user(UserRole.ADMIN, finance));

        assertEquals(2L, financeResult.getReimbursementCount());
        assertEquals(2, financeResult.getBudgets().size());
        assertEquals(2L, adminResult.getReimbursementCount());
        assertEquals(2, adminResult.getBudgets().size());
    }

    @Test
    void cashierCanViewApprovedPaymentTasksButOfficeCannot() {
        AppUser cashier = user(UserRole.CASHIER, department(2L, "Finance"));
        AppUser office = user(UserRole.OFFICE, department(4L, "Office"));
        when(reimbursementRepository.search(null, null)).thenReturn(List.of(
                reimbursement(department(1L, "Research"), "256.80", ReimbursementStatus.EXECUTIVE_APPROVED)
        ));

        assertEquals(1, service.paymentTasks(cashier).size());
        assertThrows(ForbiddenException.class, () -> service.paymentTasks(office));
    }

    @Test
    void createGeneratesDailyApprovalNumberAndCopiesInternalVoucherFields() {
        AppUser employee = user(UserRole.EMPLOYEE, department(1L, "Research"));
        ReflectionTestUtils.setField(employee, "id", 8L);
        ReimbursementRequest request = reimbursementRequest("256.80");
        request.setApplicantPhone("13800000000");
        request.setBudgetNumber("YS-2026-001");
        request.setPayeeName("测试收款人");
        request.setPaymentTotal(new BigDecimal("256.80"));
        when(reimbursementRepository.findTopByApprovalNumberStartingWithOrderByApprovalNumberDesc(any()))
                .thenReturn(Optional.empty());
        when(reimbursementRepository.save(any(Reimbursement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReimbursementResponse response = service.create(employee, request);

        assertTrue(response.getApprovalNumber().matches("BX\\d{8}001"));
        assertEquals("YS-2026-001", response.getBudgetNumber());
        assertEquals("测试收款人", response.getPayeeName());
    }

    @Test
    void highValueReimbursementRequiresExplanationAndMeetingMaterial() {
        Department research = department(1L, "Research");
        AppUser employee = user(UserRole.EMPLOYEE, research);
        ReflectionTestUtils.setField(employee, "id", 8L);
        Reimbursement reimbursement = reimbursement(research, "50000.01", ReimbursementStatus.DRAFT);
        ReflectionTestUtils.setField(reimbursement, "id", 99L);
        reimbursement.setApplicant(employee);
        reimbursement.setHighValueExplanation("院务会议审议通过");
        when(reimbursementRepository.findDetailById(99L)).thenReturn(Optional.of(reimbursement));
        when(attachmentRepository.existsByReimbursementIdAndAttachmentType(99L, com.geekworkshop.finance.entity.AttachmentType.MEETING_MINUTES))
                .thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.submit(employee, 99L));

        assertTrue(exception.getMessage().contains("meeting review material"));
        verify(attachmentRepository).existsByReimbursementIdAndAttachmentType(
                99L,
                com.geekworkshop.finance.entity.AttachmentType.MEETING_MINUTES
        );
    }

    @Test
    void financeInitialReviewMovesToDepartmentApproval() {
        Department research = department(1L, "Research");
        AppUser finance = user(UserRole.FINANCE, department(2L, "Finance"));
        Reimbursement reimbursement = reimbursement(research, "1200.00", ReimbursementStatus.SUBMITTED);
        ReflectionTestUtils.setField(reimbursement, "id", 11L);
        when(reimbursementRepository.findDetailById(11L)).thenReturn(Optional.of(reimbursement));
        when(reimbursementRepository.save(reimbursement)).thenReturn(reimbursement);
        ApprovalRequest request = approval(ApprovalAction.APPROVE, "票据完整");

        ReimbursementResponse response = service.approve(finance, 11L, request);

        assertEquals(ReimbursementStatus.FINANCE_INITIAL_APPROVED, response.getStatus());
    }

    @Test
    void rejectionRequiresReasonAndReturnsApplicationToDraft() {
        Department research = department(1L, "Research");
        AppUser finance = user(UserRole.FINANCE, department(2L, "Finance"));
        Reimbursement reimbursement = reimbursement(research, "1200.00", ReimbursementStatus.SUBMITTED);
        ReflectionTestUtils.setField(reimbursement, "id", 12L);
        when(reimbursementRepository.findDetailById(12L)).thenReturn(Optional.of(reimbursement));

        assertThrows(BusinessException.class, () -> service.approve(finance, 12L, approval(ApprovalAction.REJECT, "")));

        when(reimbursementRepository.save(reimbursement)).thenReturn(reimbursement);
        ReimbursementResponse response = service.approve(finance, 12L, approval(ApprovalAction.REJECT, "合同附件缺失"));
        assertEquals(ReimbursementStatus.DRAFT, response.getStatus());
    }

    @Test
    void cashierMustUploadBankReceiptBeforePayment() {
        Department research = department(1L, "Research");
        AppUser cashier = user(UserRole.CASHIER, department(2L, "Finance"));
        Reimbursement reimbursement = reimbursement(research, "800.00", ReimbursementStatus.EXECUTIVE_APPROVED);
        ReflectionTestUtils.setField(reimbursement, "id", 13L);
        when(reimbursementRepository.findDetailById(13L)).thenReturn(Optional.of(reimbursement));
        when(attachmentRepository.existsByReimbursementIdAndAttachmentType(13L, AttachmentType.BANK_RECEIPT))
                .thenReturn(false, true);
        PaymentRequest payment = payment("800.00");

        assertThrows(BusinessException.class, () -> service.confirmPayment(cashier, 13L, payment));

        when(reimbursementRepository.save(reimbursement)).thenReturn(reimbursement);
        ReimbursementResponse response = service.confirmPayment(cashier, 13L, payment);
        assertEquals(ReimbursementStatus.PAID, response.getStatus());
        assertEquals("PAY-2026-001", response.getPaymentVoucherNumber());
    }

    @Test
    void financeRecheckDeductsBudgetAndCompletesFlow() {
        Department research = department(1L, "Research");
        AppUser finance = user(UserRole.FINANCE, department(2L, "Finance"));
        Reimbursement reimbursement = reimbursement(research, "800.00", ReimbursementStatus.PAID);
        ReflectionTestUtils.setField(reimbursement, "id", 14L);
        Budget budget = budget(research, "5000.00");
        when(reimbursementRepository.findDetailById(14L)).thenReturn(Optional.of(reimbursement));
        when(budgetRepository.findByDepartmentIdAndBudgetYear(1L, Year.now().getValue())).thenReturn(Optional.of(budget));
        when(reimbursementRepository.save(reimbursement)).thenReturn(reimbursement);

        ReimbursementResponse response = service.approve(finance, 14L, approval(ApprovalAction.APPROVE, "付款信息无误"));

        assertEquals(ReimbursementStatus.COMPLETED, response.getStatus());
        assertEquals(new BigDecimal("800.00"), budget.getUsedAmount());
        assertEquals(new BigDecimal("4200.00"), budget.getRemainingAmount());
    }

    private ApprovalRequest approval(ApprovalAction action, String comment) {
        ApprovalRequest request = new ApprovalRequest();
        request.setAction(action);
        request.setComment(comment);
        return request;
    }

    private PaymentRequest payment(String amount) {
        PaymentRequest request = new PaymentRequest();
        request.setPaymentDate(LocalDate.now());
        request.setPaymentAmount(new BigDecimal(amount));
        request.setVoucherNumber("PAY-2026-001");
        request.setComment("银行转账完成");
        return request;
    }

    private ReimbursementRequest reimbursementRequest(String amount) {
        ReimbursementRequest request = new ReimbursementRequest();
        request.setTitle("测试报销");
        request.setExpenseType("交通费");
        request.setAmount(new BigDecimal(amount));
        request.setExpenseDate(LocalDate.now());
        return request;
    }

    private AppUser user(UserRole role, Department department) {
        AppUser user = new AppUser();
        user.setRole(role);
        user.setDepartment(department);
        return user;
    }

    private Department department(Long id, String name) {
        Department department = new Department();
        ReflectionTestUtils.setField(department, "id", id);
        department.setCode(name.toUpperCase());
        department.setName(name);
        return department;
    }

    private Reimbursement reimbursement(
            Department department,
            String amount,
            ReimbursementStatus status
    ) {
        Reimbursement reimbursement = new Reimbursement();
        reimbursement.setDepartment(department);
        reimbursement.setAmount(new BigDecimal(amount));
        reimbursement.setExpenseDate(LocalDate.now());
        reimbursement.setStatus(status);
        return reimbursement;
    }

    private Budget budget(Department department, String totalAmount) {
        Budget budget = new Budget();
        budget.setDepartment(department);
        budget.setBudgetYear(Year.now().getValue());
        budget.setTotalAmount(new BigDecimal(totalAmount));
        budget.setUsedAmount(BigDecimal.ZERO);
        budget.setRemainingAmount(new BigDecimal(totalAmount));
        return budget;
    }
}
