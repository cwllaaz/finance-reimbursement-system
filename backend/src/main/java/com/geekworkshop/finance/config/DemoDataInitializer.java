package com.geekworkshop.finance.config;

import com.geekworkshop.finance.entity.*;
import com.geekworkshop.finance.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Component
@Order(20)
public class DemoDataInitializer implements CommandLineRunner {
    private static final byte[] DEMO_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");

    private final AppUserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final ReimbursementRepository reimbursementRepository;
    private final ApprovalRecordRepository reimbursementApprovalRepository;
    private final AttachmentRepository reimbursementAttachmentRepository;
    private final PurchaseApplicationRepository purchaseRepository;
    private final PurchaseApprovalRecordRepository purchaseApprovalRepository;
    private final PurchaseAttachmentRepository purchaseAttachmentRepository;
    private final LaborApplicationRepository laborRepository;
    private final LaborApprovalRecordRepository laborApprovalRepository;
    private final LaborAttachmentRepository laborAttachmentRepository;
    private final AdvanceApplicationRepository advanceRepository;
    private final AdvanceApprovalRecordRepository advanceApprovalRepository;
    private final AdvanceAttachmentRepository advanceAttachmentRepository;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    public DemoDataInitializer(
            AppUserRepository userRepository,
            DepartmentRepository departmentRepository,
            ReimbursementRepository reimbursementRepository,
            ApprovalRecordRepository reimbursementApprovalRepository,
            AttachmentRepository reimbursementAttachmentRepository,
            PurchaseApplicationRepository purchaseRepository,
            PurchaseApprovalRecordRepository purchaseApprovalRepository,
            PurchaseAttachmentRepository purchaseAttachmentRepository,
            LaborApplicationRepository laborRepository,
            LaborApprovalRecordRepository laborApprovalRepository,
            LaborAttachmentRepository laborAttachmentRepository,
            AdvanceApplicationRepository advanceRepository,
            AdvanceApprovalRecordRepository advanceApprovalRepository,
            AdvanceAttachmentRepository advanceAttachmentRepository
    ) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.reimbursementRepository = reimbursementRepository;
        this.reimbursementApprovalRepository = reimbursementApprovalRepository;
        this.reimbursementAttachmentRepository = reimbursementAttachmentRepository;
        this.purchaseRepository = purchaseRepository;
        this.purchaseApprovalRepository = purchaseApprovalRepository;
        this.purchaseAttachmentRepository = purchaseAttachmentRepository;
        this.laborRepository = laborRepository;
        this.laborApprovalRepository = laborApprovalRepository;
        this.laborAttachmentRepository = laborAttachmentRepository;
        this.advanceRepository = advanceRepository;
        this.advanceApprovalRepository = advanceApprovalRepository;
        this.advanceAttachmentRepository = advanceAttachmentRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        AppUser employee = user("employee");
        AppUser manager = user("manager");
        AppUser finance = user("finance");
        AppUser executive = user("executive");
        AppUser cashier = user("cashier");
        Department research = departmentRepository.findByCode("RESEARCH").orElseThrow();

        ensureReimbursements(employee, manager, finance, executive, cashier, research);
        ensurePurchases(employee, manager, finance, executive, research);
        ensureLabor(employee, manager, finance, executive, cashier, research);
        ensureAdvances(employee, manager, finance, executive, cashier, research);
    }

    private void ensureReimbursements(
            AppUser employee, AppUser manager, AppUser finance, AppUser executive, AppUser cashier,
            Department department
    ) {
        reimbursement("BX20260101001", "演示报销-草稿", "860.00", ReimbursementStatus.DRAFT,
                employee, department);

        Reimbursement approval = reimbursement(
                "BX20260101002", "演示报销-部门审批中", "1280.00",
                ReimbursementStatus.FINANCE_INITIAL_APPROVED, employee, department);
        approval(approval, finance, "FINANCE_INITIAL", "财务初审通过，票据齐全");

        Reimbursement payment = reimbursement(
                "BX20260101003", "演示报销-待出纳付款", "4600.00",
                ReimbursementStatus.EXECUTIVE_APPROVED, employee, department);
        reimbursementApprovedNodes(payment, finance, manager, executive);

        Reimbursement completed = reimbursement(
                "BX20260101004", "演示报销-大额项目已完成", "68000.00",
                ReimbursementStatus.COMPLETED, employee, department);
        completed.setHighValueExplanation("院务委员会审议通过，用于科研设备及配套服务支出。");
        completed.setPaymentDate(LocalDate.now().minusDays(2));
        completed.setPaymentTotal(completed.getAmount());
        completed.setPaymentVoucherNumber("BX-PAY-DEMO-001");
        completed.setSubmittedAt(LocalDateTime.now().minusDays(8));
        reimbursementRepository.save(completed);
        reimbursementApprovedNodes(completed, finance, manager, executive);
        approval(completed, cashier, "CASHIER_PAYMENT", "已完成银行付款并上传回执");
        approval(completed, finance, "FINANCE_RECHECK", "付款金额与审批金额一致，复核完成");
        reimbursementAttachment(completed, AttachmentType.MEETING_MINUTES,
                "demo-reimbursement-meeting.png", "meeting");
        reimbursementAttachment(completed, AttachmentType.BANK_RECEIPT,
                "demo-reimbursement-receipt.png", "bank_receipt");
    }

    private void reimbursementApprovedNodes(
            Reimbursement value, AppUser finance, AppUser manager, AppUser executive
    ) {
        approval(value, finance, "FINANCE_INITIAL", "财务初审通过");
        approval(value, manager, "DEPARTMENT", "部门负责人审批通过");
        approval(value, executive, "EXECUTIVE", "执行院长审批通过");
    }

    private Reimbursement reimbursement(
            String number, String title, String amount, ReimbursementStatus status,
            AppUser applicant, Department department
    ) {
        return reimbursementRepository.findByApprovalNumber(number).orElseGet(() -> {
            Reimbursement value = new Reimbursement();
            value.setApprovalNumber(number);
            value.setApplicant(applicant);
            value.setDepartment(department);
            value.setTitle(title);
            value.setExpenseType("演示费用");
            value.setAmount(new BigDecimal(amount));
            value.setExpenseDate(LocalDate.now().minusDays(6));
            value.setDescription("用于系统验收和角色流程展示");
            value.setApplicantPhone("13800000001");
            value.setBudgetNumber("DEMO-BUDGET-2026");
            value.setReimbursementReason(title);
            value.setPayeeName(applicant.getRealName());
            value.setBankAccount("6222020000000000001");
            value.setBankName("中国工商银行");
            value.setPaymentTotal(value.getAmount());
            value.setStatus(status);
            if (status != ReimbursementStatus.DRAFT) {
                value.setSubmittedAt(LocalDateTime.now().minusDays(5));
            }
            return reimbursementRepository.save(value);
        });
    }

    private void approval(Reimbursement value, AppUser user, String node, String comment) {
        boolean exists = reimbursementApprovalRepository
                .findByReimbursementIdOrderByCreatedAtAsc(value.getId()).stream()
                .anyMatch(record -> node.equals(record.getApprovalNode()));
        if (exists) return;
        ApprovalRecord record = new ApprovalRecord();
        record.setReimbursement(value);
        record.setApprover(user);
        record.setApprovalNode(node);
        record.setAction(ApprovalAction.APPROVE);
        record.setComment(comment);
        reimbursementApprovalRepository.save(record);
    }

    private void ensurePurchases(
            AppUser employee, AppUser manager, AppUser finance, AppUser executive, Department department
    ) {
        purchase("CG20260101001", "演示申购-草稿", "2400.00", PurchaseStatus.DRAFT, employee, department);

        PurchaseApplication approval = purchase(
                "CG20260101002", "演示申购-部门审批中", "9800.00",
                PurchaseStatus.FINANCE_APPROVED, employee, department);
        purchaseApproval(approval, finance, "FINANCE", "预算审核通过");

        PurchaseApplication awaitingExecutive = purchase(
                "CG20260101003", "演示申购-待执行院长审批", "16800.00",
                PurchaseStatus.DEPARTMENT_APPROVED, employee, department);
        purchaseApproval(awaitingExecutive, finance, "FINANCE", "财务审核通过");
        purchaseApproval(awaitingExecutive, manager, "DEPARTMENT", "部门负责人审批通过");

        PurchaseApplication completed = purchase(
                "CG20260101004", "演示申购-大额设备采购已完成", "62000.00",
                PurchaseStatus.COMPLETED, employee, department);
        purchaseApproval(completed, finance, "FINANCE", "预算充足");
        purchaseApproval(completed, manager, "DEPARTMENT", "同意采购");
        purchaseApproval(completed, executive, "EXECUTIVE", "院务会议审议后同意");
        purchaseAttachment(completed, PurchaseAttachmentType.MEETING_MINUTES,
                "demo-purchase-meeting.png", "purchase_meeting");
    }

    private PurchaseApplication purchase(
            String number, String reason, String amount, PurchaseStatus status,
            AppUser applicant, Department department
    ) {
        return purchaseRepository.findByApplicationNumber(number).orElseGet(() -> {
            PurchaseApplication value = new PurchaseApplication();
            value.setApplicationNumber(number);
            value.setApplicant(applicant);
            value.setDepartment(department);
            value.setApplicantPhone("13800000001");
            value.setBudgetNumber("DEMO-BUDGET-2026");
            value.setPurchaseMethod("询价采购");
            value.setTaxExempt(false);
            value.setUseLocation("科研楼演示实验室");
            value.setPurchaseReason(reason);
            value.setAmount(new BigDecimal(amount));
            value.setStatus(status);
            if (status != PurchaseStatus.DRAFT) value.setSubmittedAt(LocalDateTime.now().minusDays(5));
            PurchaseItem item = new PurchaseItem();
            item.setPurchaseApplication(value);
            item.setItemName(reason.contains("大额") ? "科研工作站" : "办公设备");
            item.setSpecification("演示规格");
            item.setManufacturer("演示厂商");
            item.setUnitPrice(value.getAmount());
            item.setQuantity(1);
            item.setTotalPrice(value.getAmount());
            value.getItems().add(item);
            return purchaseRepository.save(value);
        });
    }

    private void purchaseApproval(PurchaseApplication value, AppUser user, String node, String comment) {
        boolean exists = purchaseApprovalRepository
                .findByPurchaseApplicationIdOrderByCreatedAtAsc(value.getId()).stream()
                .anyMatch(record -> node.equals(record.getApprovalNode()));
        if (exists) return;
        PurchaseApprovalRecord record = new PurchaseApprovalRecord();
        record.setPurchaseApplication(value);
        record.setApprover(user);
        record.setApprovalNode(node);
        record.setAction(ApprovalAction.APPROVE);
        record.setComment(comment);
        purchaseApprovalRepository.save(record);
    }

    private void ensureLabor(
            AppUser employee, AppUser manager, AppUser finance, AppUser executive, AppUser cashier,
            Department department
    ) {
        labor("LW20260101001", "演示劳务-草稿", "1800.00", LaborStatus.DRAFT, employee, department);

        LaborApplication approval = labor(
                "LW20260101002", "演示劳务-部门审批中", "3200.00",
                LaborStatus.FINANCE_INITIAL_APPROVED, employee, department);
        laborApproval(approval, finance, "FINANCE_INITIAL", "财务初审通过");

        LaborApplication payment = labor(
                "LW20260101003", "演示劳务-待出纳付款", "5600.00",
                LaborStatus.EXECUTIVE_APPROVED, employee, department);
        laborApprovedNodes(payment, finance, manager, executive);

        LaborApplication completed = labor(
                "LW20260101004", "演示劳务-专家咨询费已完成", "8000.00",
                LaborStatus.COMPLETED, employee, department);
        completed.setPaymentDate(LocalDate.now().minusDays(2));
        completed.setPaymentAmount(completed.getTotalAmount());
        completed.setPaymentVoucherNumber("LW-PAY-DEMO-001");
        laborRepository.save(completed);
        laborApprovedNodes(completed, finance, manager, executive);
        laborApproval(completed, cashier, "CASHIER_PAYMENT", "出纳付款完成");
        laborApproval(completed, finance, "FINANCE_RECHECK", "财务复核完成");
        laborAttachment(completed, LaborAttachmentType.BANK_RECEIPT,
                "demo-labor-receipt.png", "labor_receipt");
    }

    private void laborApprovedNodes(
            LaborApplication value, AppUser finance, AppUser manager, AppUser executive
    ) {
        laborApproval(value, finance, "FINANCE_INITIAL", "财务初审通过");
        laborApproval(value, manager, "DEPARTMENT", "部门负责人审批通过");
        laborApproval(value, executive, "EXECUTIVE", "执行院长审批通过");
    }

    private LaborApplication labor(
            String number, String title, String amount, LaborStatus status,
            AppUser applicant, Department department
    ) {
        return laborRepository.findByApplicationNumber(number).orElseGet(() -> {
            LaborApplication value = new LaborApplication();
            value.setApplicationNumber(number);
            value.setCategory(LaborCategory.RESEARCH_LABOR);
            value.setTitle(title);
            value.setDescription("演示劳务费用");
            value.setBudgetNumber("DEMO-BUDGET-2026");
            value.setTotalAmount(new BigDecimal(amount));
            value.setAmountInWords("演示金额");
            value.setStatus(status);
            value.setApplicant(applicant);
            value.setDepartment(department);
            if (status != LaborStatus.DRAFT) value.setSubmittedAt(LocalDateTime.now().minusDays(5));
            LaborRecipient recipient = new LaborRecipient();
            recipient.setLaborApplication(value);
            recipient.setName("演示专家");
            recipient.setPhone("13900000001");
            recipient.setIdCard("110101199001010011");
            recipient.setOrganization("示例研究机构");
            recipient.setPosition("专家");
            recipient.setServiceContent(title);
            recipient.setNetAmount(value.getTotalAmount());
            recipient.setBankAccount("6222020000000000022");
            recipient.setBankName("中国银行");
            value.getRecipients().add(recipient);
            return laborRepository.save(value);
        });
    }

    private void laborApproval(LaborApplication value, AppUser user, String node, String comment) {
        boolean exists = laborApprovalRepository.findDetailsByApplicationId(value.getId()).stream()
                .anyMatch(record -> node.equals(record.getApprovalNode()));
        if (exists) return;
        LaborApprovalRecord record = new LaborApprovalRecord();
        record.setLaborApplication(value);
        record.setApprover(user);
        record.setApprovalNode(node);
        record.setAction(ApprovalAction.APPROVE);
        record.setComment(comment);
        laborApprovalRepository.save(record);
    }

    private void ensureAdvances(
            AppUser employee, AppUser manager, AppUser finance, AppUser executive, AppUser cashier,
            Department department
    ) {
        advance("YF20260101001", "演示借款-草稿", "3000.00", AdvanceStatus.DRAFT, employee, department);

        AdvanceApplication approval = advance(
                "YF20260101002", "演示借款-财务审批中", "6000.00",
                AdvanceStatus.DEPARTMENT_APPROVED, employee, department);
        advanceApproval(approval, manager, "DEPARTMENT", "部门负责人审批通过");

        AdvanceApplication payment = advance(
                "YF20260101003", "演示预付款-待出纳付款", "12000.00",
                AdvanceStatus.EXECUTIVE_APPROVED, employee, department);
        advanceApprovedNodes(payment, manager, finance, executive);

        AdvanceApplication completed = advance(
                "YF20260101004", "演示借款-付款复核已完成", "15000.00",
                AdvanceStatus.COMPLETED, employee, department);
        completed.setPaymentDate(LocalDate.now().minusDays(2));
        completed.setPaymentAmount(completed.getAmount());
        completed.setPaymentVoucherNumber("YF-PAY-DEMO-001");
        completed.setSettlementStatus(SettlementStatus.PENDING_OFFSET);
        advanceRepository.save(completed);
        advanceApprovedNodes(completed, manager, finance, executive);
        advanceApproval(completed, cashier, "CASHIER_PAYMENT", "出纳付款完成");
        advanceApproval(completed, finance, "FINANCE_RECHECK", "财务复核完成");
        advanceAttachment(completed, AdvanceAttachmentType.BANK_RECEIPT,
                "demo-advance-receipt.png", "advance_receipt");
    }

    private void advanceApprovedNodes(
            AdvanceApplication value, AppUser manager, AppUser finance, AppUser executive
    ) {
        advanceApproval(value, manager, "DEPARTMENT", "部门负责人审批通过");
        advanceApproval(value, finance, "FINANCE", "财务审核通过");
        advanceApproval(value, executive, "EXECUTIVE", "执行院长审批通过");
    }

    private AdvanceApplication advance(
            String number, String reason, String amount, AdvanceStatus status,
            AppUser applicant, Department department
    ) {
        return advanceRepository.findByApplicationNumber(number).orElseGet(() -> {
            AdvanceApplication value = new AdvanceApplication();
            value.setApplicationNumber(number);
            value.setType(number.endsWith("003") ? AdvanceType.PREPAYMENT : AdvanceType.TEMPORARY_LOAN);
            value.setReason(reason);
            value.setAmount(new BigDecimal(amount));
            value.setPaymentMethod("银行转账");
            value.setPayeeName(applicant.getRealName());
            value.setBankAccount("6222020000000000033");
            value.setBankName("中国建设银行");
            value.setExpectedRepaymentDate(LocalDate.now().plusDays(30));
            if (value.getType() == AdvanceType.PREPAYMENT) {
                value.setExpectedRepaymentDate(null);
                value.setPartnerName("演示合作单位");
                value.setExpectedSettlementDate(LocalDate.now().plusDays(30));
            }
            value.setStatus(status);
            value.setOffsetAmount(BigDecimal.ZERO);
            value.setApplicant(applicant);
            value.setDepartment(department);
            if (status != AdvanceStatus.DRAFT) value.setSubmittedAt(LocalDateTime.now().minusDays(5));
            return advanceRepository.save(value);
        });
    }

    private void advanceApproval(AdvanceApplication value, AppUser user, String node, String comment) {
        boolean exists = advanceApprovalRepository.findDetailsByApplicationId(value.getId()).stream()
                .anyMatch(record -> node.equals(record.getApprovalNode()));
        if (exists) return;
        AdvanceApprovalRecord record = new AdvanceApprovalRecord();
        record.setAdvanceApplication(value);
        record.setApprover(user);
        record.setApprovalNode(node);
        record.setAction(ApprovalAction.APPROVE);
        record.setComment(comment);
        advanceApprovalRepository.save(record);
    }

    private void reimbursementAttachment(
            Reimbursement value, AttachmentType type, String fileName, String directory
    ) {
        if (reimbursementAttachmentRepository.existsByReimbursementIdAndAttachmentType(value.getId(), type)) return;
        Attachment attachment = new Attachment();
        attachment.setReimbursement(value);
        attachment.setAttachmentType(type);
        fill(attachment, fileName, demoFile(directory, fileName));
        reimbursementAttachmentRepository.save(attachment);
    }

    private void purchaseAttachment(
            PurchaseApplication value, PurchaseAttachmentType type, String fileName, String directory
    ) {
        if (purchaseAttachmentRepository.existsByPurchaseApplicationIdAndAttachmentType(value.getId(), type)) return;
        PurchaseAttachment attachment = new PurchaseAttachment();
        attachment.setPurchaseApplication(value);
        attachment.setAttachmentType(type);
        attachment.setFileName(fileName);
        attachment.setFileUrl(demoFile(directory, fileName));
        attachment.setFileType("image/png");
        attachment.setFileSize((long) DEMO_PNG.length);
        purchaseAttachmentRepository.save(attachment);
    }

    private void laborAttachment(
            LaborApplication value, LaborAttachmentType type, String fileName, String directory
    ) {
        if (laborAttachmentRepository.existsByLaborApplicationIdAndAttachmentType(value.getId(), type)) return;
        LaborAttachment attachment = new LaborAttachment();
        attachment.setLaborApplication(value);
        attachment.setAttachmentType(type);
        attachment.setFileName(fileName);
        attachment.setFileUrl(demoFile(directory, fileName));
        attachment.setFileType("image/png");
        attachment.setFileSize((long) DEMO_PNG.length);
        laborAttachmentRepository.save(attachment);
    }

    private void advanceAttachment(
            AdvanceApplication value, AdvanceAttachmentType type, String fileName, String directory
    ) {
        if (advanceAttachmentRepository.existsByAdvanceApplicationIdAndAttachmentType(value.getId(), type)) return;
        AdvanceAttachment attachment = new AdvanceAttachment();
        attachment.setAdvanceApplication(value);
        attachment.setAttachmentType(type);
        attachment.setFileName(fileName);
        attachment.setFileUrl(demoFile(directory, fileName));
        attachment.setFileType("image/png");
        attachment.setFileSize((long) DEMO_PNG.length);
        advanceAttachmentRepository.save(attachment);
    }

    private void fill(Attachment attachment, String fileName, String fileUrl) {
        attachment.setFileName(fileName);
        attachment.setFileUrl(fileUrl);
        attachment.setFileType("image/png");
        attachment.setFileSize((long) DEMO_PNG.length);
    }

    private String demoFile(String directory, String fileName) {
        try {
            Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path folder = root.resolve(directory).normalize();
            if (!folder.startsWith(root)) throw new IllegalStateException("invalid demo attachment path");
            Files.createDirectories(folder);
            Path file = folder.resolve(fileName).normalize();
            if (!Files.exists(file)) Files.write(file, DEMO_PNG);
            return "/uploads/" + directory + "/" + fileName;
        } catch (Exception exception) {
            throw new IllegalStateException("failed to create demo attachment", exception);
        }
    }

    private AppUser user(String username) {
        return userRepository.findByUsername(username).orElseThrow();
    }
}
