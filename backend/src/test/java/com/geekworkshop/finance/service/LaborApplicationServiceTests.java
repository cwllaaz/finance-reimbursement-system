package com.geekworkshop.finance.service;

import com.geekworkshop.finance.dto.*;
import com.geekworkshop.finance.entity.*;
import com.geekworkshop.finance.exception.BusinessException;
import com.geekworkshop.finance.exception.ForbiddenException;
import com.geekworkshop.finance.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LaborApplicationServiceTests {
    @Mock LaborApplicationRepository applicationRepository;
    @Mock LaborAttachmentRepository attachmentRepository;
    @Mock LaborApprovalRecordRepository approvalRepository;
    @Mock OperationLogService operationLogService;
    private LaborApplicationService service;

    @BeforeEach
    void setUp() {
        service = new LaborApplicationService(applicationRepository, attachmentRepository,
                approvalRepository, operationLogService);
        lenient().when(applicationRepository.save(any())).thenAnswer(invocation -> {
            LaborApplication value = invocation.getArgument(0);
            if (value.getId() == null) ReflectionTestUtils.setField(value, "id", 10L);
            return value;
        });
        lenient().when(applicationRepository.findTopByApplicationNumberStartingWithOrderByApplicationNumberDesc(any()))
                .thenReturn(Optional.empty());
        lenient().when(attachmentRepository.findByLaborApplicationIdOrderByCreatedAtAsc(any())).thenReturn(List.of());
        lenient().when(approvalRepository.findDetailsByApplicationId(any())).thenReturn(List.of());
    }

    @Test
    void createGeneratesNumberTotalAndUppercaseAmount() {
        var response = service.create(user(1L, UserRole.EMPLOYEE), request());

        assertTrue(response.applicationNumber().startsWith("LW"));
        assertEquals(new BigDecimal("1200.50"), response.totalAmount());
        assertEquals("人民币壹仟贰佰元伍角", response.amountInWords());
        assertEquals(2, response.recipients().size());
    }

    @Test
    void officeCannotAccessLaborModule() {
        AppUser office = user(6L, UserRole.OFFICE);

        assertThrows(ForbiddenException.class, () -> service.list(office, null, null));
        assertThrows(ForbiddenException.class, () -> service.create(office, request()));
        verify(applicationRepository, never()).findAllDetails();
    }

    @Test
    void committeeCanViewLaborApplicationsFromAllDepartments() {
        LaborApplication first = application(LaborStatus.SUBMITTED);
        LaborApplication second = application(LaborStatus.COMPLETED);
        ReflectionTestUtils.setField(second, "id", 11L);
        Department other = new Department();
        ReflectionTestUtils.setField(other, "id", 99L);
        other.setName("Other");
        second.setDepartment(other);
        when(applicationRepository.findAllDetails()).thenReturn(List.of(first, second));

        assertEquals(2, service.list(user(7L, UserRole.COMMITTEE), null, null).size());
        assertThrows(ForbiddenException.class,
                () -> service.create(user(7L, UserRole.COMMITTEE), request()));
    }

    @Test
    void listMasksIdCardAndBankAccount() {
        LaborApplication value = application(LaborStatus.DRAFT);
        when(applicationRepository.findAllDetails()).thenReturn(List.of(value));

        var response = service.list(value.getApplicant(), null, null).getFirst();

        assertEquals("420***********1234", response.recipients().getFirst().idCard());
        assertEquals("6222 **** **** 8888", response.recipients().getFirst().bankAccount());
    }

    @Test
    void departmentManagerCannotApproveOtherDepartment() {
        LaborApplication value = application(LaborStatus.FINANCE_INITIAL_APPROVED);
        when(applicationRepository.findDetailById(10L)).thenReturn(Optional.of(value));
        AppUser manager = user(2L, UserRole.DEPARTMENT_MANAGER);
        Department other = new Department();
        ReflectionTestUtils.setField(other, "id", 99L);
        other.setName("其他部门");
        manager.setDepartment(other);
        ApprovalRequest request = new ApprovalRequest();
        request.setAction(ApprovalAction.APPROVE);

        assertThrows(ForbiddenException.class, () -> service.approve(manager, 10L, request));
    }

    @Test
    void cashierNeedsBankReceiptBeforePayment() {
        LaborApplication value = application(LaborStatus.EXECUTIVE_APPROVED);
        when(applicationRepository.findDetailById(10L)).thenReturn(Optional.of(value));
        when(attachmentRepository.existsByLaborApplicationIdAndAttachmentType(
                10L, LaborAttachmentType.BANK_RECEIPT)).thenReturn(false);
        PaymentRequest request = paymentRequest(value.getTotalAmount());

        assertThrows(BusinessException.class,
                () -> service.confirmPayment(user(3L, UserRole.CASHIER), 10L, request));
    }

    @Test
    void paymentThenFinanceRecheckCompletesFlow() {
        LaborApplication value = application(LaborStatus.EXECUTIVE_APPROVED);
        when(applicationRepository.findDetailById(10L)).thenReturn(Optional.of(value));
        when(attachmentRepository.existsByLaborApplicationIdAndAttachmentType(
                10L, LaborAttachmentType.BANK_RECEIPT)).thenReturn(true);

        service.confirmPayment(user(3L, UserRole.CASHIER), 10L, paymentRequest(value.getTotalAmount()));
        assertEquals(LaborStatus.PAID, value.getStatus());

        ApprovalRequest approval = new ApprovalRequest();
        approval.setAction(ApprovalAction.APPROVE);
        approval.setComment("复核无误");
        service.approve(user(4L, UserRole.FINANCE), 10L, approval);
        assertEquals(LaborStatus.COMPLETED, value.getStatus());
        verify(approvalRepository, atLeast(2)).save(any(LaborApprovalRecord.class));
    }

    private LaborApplicationRequest request() {
        LaborApplicationRequest request = new LaborApplicationRequest();
        request.setCategory(LaborCategory.RESEARCH_LABOR);
        request.setTitle("课题数据整理劳务");
        request.setRecipients(List.of(
                recipient("张三", "800.00", "420100200001011234", "6222000012348888"),
                recipient("李四", "400.50", "420100200001015678", "6222000012349999")
        ));
        return request;
    }

    private LaborRecipientRequest recipient(String name, String amount, String idCard, String bank) {
        LaborRecipientRequest value = new LaborRecipientRequest();
        value.setName(name);
        value.setPhone("13800000000");
        value.setIdCard(idCard);
        value.setOrganization("示例研究院");
        value.setPosition("研究助理");
        value.setServiceContent("科研数据整理");
        value.setNetAmount(new BigDecimal(amount));
        value.setBankAccount(bank);
        value.setBankName("中国银行");
        return value;
    }

    private LaborApplication application(LaborStatus status) {
        LaborApplication value = new LaborApplication();
        ReflectionTestUtils.setField(value, "id", 10L);
        ReflectionTestUtils.setField(value, "createdAt", java.time.LocalDateTime.now());
        ReflectionTestUtils.setField(value, "updatedAt", java.time.LocalDateTime.now());
        value.setApplicationNumber("LW20260630001");
        value.setCategory(LaborCategory.RESEARCH_LABOR);
        value.setTitle("课题数据整理劳务");
        value.setApplicant(user(1L, UserRole.EMPLOYEE));
        value.setDepartment(value.getApplicant().getDepartment());
        value.setStatus(status);
        value.setTotalAmount(new BigDecimal("800.00"));
        value.setAmountInWords("人民币捌佰元整");
        LaborRecipient item = new LaborRecipient();
        item.setLaborApplication(value);
        item.setName("张三");
        item.setIdCard("420100200001011234");
        item.setServiceContent("科研数据整理");
        item.setNetAmount(new BigDecimal("800.00"));
        item.setBankAccount("6222000012348888");
        item.setBankName("中国银行");
        value.getRecipients().add(item);
        return value;
    }

    private PaymentRequest paymentRequest(BigDecimal amount) {
        PaymentRequest request = new PaymentRequest();
        request.setPaymentDate(LocalDate.now());
        request.setPaymentAmount(amount);
        request.setVoucherNumber("PAY20260630001");
        return request;
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
