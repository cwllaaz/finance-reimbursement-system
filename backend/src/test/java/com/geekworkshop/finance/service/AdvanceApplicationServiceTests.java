package com.geekworkshop.finance.service;

import com.geekworkshop.finance.dto.*;
import com.geekworkshop.finance.entity.*;
import com.geekworkshop.finance.exception.*;
import com.geekworkshop.finance.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdvanceApplicationServiceTests {
    @Mock AdvanceApplicationRepository applicationRepository;
    @Mock AdvanceApprovalRecordRepository approvalRepository;
    @Mock AdvanceAttachmentRepository attachmentRepository;
    @Mock AdvanceOffsetRecordRepository offsetRepository;
    @Mock OperationLogService operationLogService;
    private AdvanceApplicationService service;

    @BeforeEach
    void setUp() {
        service = new AdvanceApplicationService(applicationRepository, approvalRepository,
                attachmentRepository, offsetRepository, operationLogService);
        lenient().when(applicationRepository.save(any())).thenAnswer(invocation -> {
            AdvanceApplication value = invocation.getArgument(0);
            if (value.getId() == null) ReflectionTestUtils.setField(value, "id", 10L);
            return value;
        });
        lenient().when(applicationRepository.findTopByApplicationNumberStartingWithOrderByApplicationNumberDesc(any()))
                .thenReturn(Optional.empty());
        lenient().when(attachmentRepository.findByAdvanceApplicationIdOrderByCreatedAtAsc(any())).thenReturn(List.of());
        lenient().when(approvalRepository.findDetailsByApplicationId(any())).thenReturn(List.of());
        lenient().when(offsetRepository.findDetailsByApplicationId(any())).thenReturn(List.of());
    }

    @Test
    void temporaryLoanRequiresRepaymentDate() {
        AdvanceApplicationRequest request = request(AdvanceType.TEMPORARY_LOAN);
        request.setExpectedRepaymentDate(null);
        assertThrows(BusinessException.class, () -> service.create(user(1L, UserRole.EMPLOYEE), request));
    }

    @Test
    void officeCannotAccessAdvanceModule() {
        AppUser office = user(6L, UserRole.OFFICE);

        assertThrows(ForbiddenException.class,
                () -> service.list(office, null, null, null, null, null));
        assertThrows(ForbiddenException.class,
                () -> service.create(office, request(AdvanceType.TEMPORARY_LOAN)));
        verify(applicationRepository, never()).findAllDetails();
    }

    @Test
    void committeeCanViewAdvanceApplicationsFromAllDepartments() {
        AdvanceApplication first = application(AdvanceStatus.SUBMITTED);
        AdvanceApplication second = application(AdvanceStatus.COMPLETED);
        ReflectionTestUtils.setField(second, "id", 11L);
        Department other = new Department();
        ReflectionTestUtils.setField(other, "id", 99L);
        other.setName("Other");
        second.setDepartment(other);
        when(applicationRepository.findAllDetails()).thenReturn(List.of(first, second));

        assertEquals(2, service.list(
                user(7L, UserRole.COMMITTEE), null, null, null, null, null).size());
        assertThrows(ForbiddenException.class,
                () -> service.create(user(7L, UserRole.COMMITTEE), request(AdvanceType.TEMPORARY_LOAN)));
    }

    @Test
    void departmentApprovalMustComeBeforeFinance() {
        AdvanceApplication value = application(AdvanceStatus.SUBMITTED);
        when(applicationRepository.findDetailById(10L)).thenReturn(Optional.of(value));
        ApprovalRequest request = approval();

        assertThrows(ForbiddenException.class, () -> service.approve(user(2L, UserRole.FINANCE), 10L, request));
        service.approve(user(3L, UserRole.DEPARTMENT_MANAGER), 10L, request);
        assertEquals(AdvanceStatus.DEPARTMENT_APPROVED, value.getStatus());
    }

    @Test
    void cashierCannotPayWithoutBankReceipt() {
        AdvanceApplication value = application(AdvanceStatus.EXECUTIVE_APPROVED);
        when(applicationRepository.findDetailById(10L)).thenReturn(Optional.of(value));
        when(attachmentRepository.existsByAdvanceApplicationIdAndAttachmentType(
                10L, AdvanceAttachmentType.BANK_RECEIPT)).thenReturn(false);
        assertThrows(BusinessException.class,
                () -> service.confirmPayment(user(4L, UserRole.CASHIER), 10L, payment()));
    }

    @Test
    void partialAndFullOffsetAreTracked() {
        AdvanceApplication value = application(AdvanceStatus.COMPLETED);
        value.setSettlementStatus(SettlementStatus.PENDING_OFFSET);
        when(applicationRepository.findDetailById(10L)).thenReturn(Optional.of(value));
        AdvanceOffsetRequest first = new AdvanceOffsetRequest();
        first.setAmount(new BigDecimal("400.00"));
        service.offset(user(2L, UserRole.FINANCE), 10L, first);
        assertEquals(SettlementStatus.PARTIAL_OFFSET, value.getSettlementStatus());
        assertEquals(new BigDecimal("400.00"), value.getOffsetAmount());

        AdvanceOffsetRequest second = new AdvanceOffsetRequest();
        second.setAmount(new BigDecimal("600.00"));
        service.offset(user(2L, UserRole.FINANCE), 10L, second);
        assertEquals(SettlementStatus.OFFSET_COMPLETED, value.getSettlementStatus());
        verify(offsetRepository, times(2)).save(any(AdvanceOffsetRecord.class));
    }

    @Test
    void managerReminderOnlyContainsOwnDepartment() {
        AppUser manager = user(3L, UserRole.DEPARTMENT_MANAGER);
        AdvanceApplication overdue = application(AdvanceStatus.COMPLETED);
        overdue.setSettlementStatus(SettlementStatus.PENDING_OFFSET);
        overdue.setExpectedRepaymentDate(LocalDate.now().minusDays(1));
        AdvanceApplication otherDepartment = application(AdvanceStatus.COMPLETED);
        otherDepartment.setSettlementStatus(SettlementStatus.PENDING_OFFSET);
        Department other = new Department();
        ReflectionTestUtils.setField(other, "id", 2L);
        other.setName("Finance");
        other.setCode("FIN");
        otherDepartment.setDepartment(other);
        when(applicationRepository.findAllDetails()).thenReturn(List.of(overdue, otherDepartment));

        var stats = service.reminderStats(manager);

        assertEquals(0, stats.pendingOffsetCount());
        assertEquals(1, stats.overdueAdvanceCount());
        assertEquals(SettlementStatus.OVERDUE, overdue.getSettlementStatus());
    }

    @Test
    void financeReminderContainsAllDepartments() {
        AdvanceApplication own = application(AdvanceStatus.COMPLETED);
        own.setSettlementStatus(SettlementStatus.PENDING_OFFSET);
        AdvanceApplication other = application(AdvanceStatus.COMPLETED);
        other.setSettlementStatus(SettlementStatus.PARTIAL_OFFSET);
        Department otherDepartment = new Department();
        ReflectionTestUtils.setField(otherDepartment, "id", 2L);
        otherDepartment.setName("Finance");
        otherDepartment.setCode("FIN");
        other.setDepartment(otherDepartment);
        when(applicationRepository.findAllDetails()).thenReturn(List.of(own, other));

        var stats = service.reminderStats(user(2L, UserRole.FINANCE));

        assertEquals(2, stats.pendingOffsetCount());
        assertEquals(0, stats.overdueAdvanceCount());
    }

    @Test
    void pendingOffsetFilterIncludesPendingAndPartialOnly() {
        AdvanceApplication pending = application(AdvanceStatus.COMPLETED);
        pending.setSettlementStatus(SettlementStatus.PENDING_OFFSET);
        AdvanceApplication partial = application(AdvanceStatus.COMPLETED);
        ReflectionTestUtils.setField(partial, "id", 11L);
        partial.setSettlementStatus(SettlementStatus.PARTIAL_OFFSET);
        AdvanceApplication completed = application(AdvanceStatus.COMPLETED);
        ReflectionTestUtils.setField(completed, "id", 12L);
        completed.setSettlementStatus(SettlementStatus.OFFSET_COMPLETED);
        when(applicationRepository.findAllDetails()).thenReturn(List.of(pending, partial, completed));

        var result = service.list(
                user(2L, UserRole.FINANCE),
                null,
                null,
                null,
                null,
                List.of(SettlementStatus.PENDING_OFFSET, SettlementStatus.PARTIAL_OFFSET)
        );

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(item ->
                item.settlementStatus() == SettlementStatus.PENDING_OFFSET
                        || item.settlementStatus() == SettlementStatus.PARTIAL_OFFSET));
    }

    @Test
    void nonDashboardRolesCannotReadReminderStats() {
        assertThrows(ForbiddenException.class,
                () -> service.reminderStats(user(1L, UserRole.EMPLOYEE)));
        assertThrows(ForbiddenException.class,
                () -> service.reminderStats(user(5L, UserRole.OFFICE)));
        assertThrows(ForbiddenException.class,
                () -> service.reminderStats(user(4L, UserRole.CASHIER)));
        verify(applicationRepository, never()).findAllDetails();
    }

    private AdvanceApplicationRequest request(AdvanceType type) {
        AdvanceApplicationRequest request = new AdvanceApplicationRequest();
        request.setType(type);
        request.setReason("项目周转");
        request.setAmount(new BigDecimal("1000.00"));
        request.setPaymentMethod("银行转账");
        request.setPayeeName("张三");
        request.setBankAccount("6222000012348888");
        request.setBankName("中国银行");
        request.setExpectedRepaymentDate(LocalDate.now().plusDays(30));
        return request;
    }
    private ApprovalRequest approval() {
        ApprovalRequest request = new ApprovalRequest();
        request.setAction(ApprovalAction.APPROVE);
        request.setComment("同意");
        return request;
    }
    private PaymentRequest payment() {
        PaymentRequest request = new PaymentRequest();
        request.setPaymentDate(LocalDate.now());
        request.setPaymentAmount(new BigDecimal("1000.00"));
        request.setVoucherNumber("PAY001");
        return request;
    }
    private AdvanceApplication application(AdvanceStatus status) {
        AdvanceApplication value = new AdvanceApplication();
        ReflectionTestUtils.setField(value, "id", 10L);
        ReflectionTestUtils.setField(value, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(value, "updatedAt", LocalDateTime.now());
        value.setApplicationNumber("YF20260630001");
        value.setType(AdvanceType.TEMPORARY_LOAN);
        value.setReason("项目周转");
        value.setAmount(new BigDecimal("1000.00"));
        value.setPaymentMethod("银行转账");
        value.setPayeeName("张三");
        value.setBankAccount("6222000012348888");
        value.setBankName("中国银行");
        value.setExpectedRepaymentDate(LocalDate.now().plusDays(30));
        value.setOffsetAmount(BigDecimal.ZERO);
        value.setApplicant(user(1L, UserRole.EMPLOYEE));
        value.setDepartment(value.getApplicant().getDepartment());
        value.setStatus(status);
        return value;
    }
    private AppUser user(Long id, UserRole role) {
        Department department = new Department();
        ReflectionTestUtils.setField(department, "id", 1L);
        department.setName("科研管理部");
        department.setCode("RD");
        AppUser user = new AppUser();
        ReflectionTestUtils.setField(user, "id", id);
        user.setUsername(role.name().toLowerCase());
        user.setRealName(role.name());
        user.setRole(role);
        user.setDepartment(department);
        return user;
    }
}
