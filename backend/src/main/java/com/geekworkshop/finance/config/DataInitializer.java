package com.geekworkshop.finance.config;

import com.geekworkshop.finance.entity.AppUser;
import com.geekworkshop.finance.entity.AdvanceApplication;
import com.geekworkshop.finance.entity.AdvanceStatus;
import com.geekworkshop.finance.entity.AdvanceType;
import com.geekworkshop.finance.entity.Asset;
import com.geekworkshop.finance.entity.AssetAcceptance;
import com.geekworkshop.finance.entity.AssetHistory;
import com.geekworkshop.finance.entity.AssetHistoryAction;
import com.geekworkshop.finance.entity.AssetStatus;
import com.geekworkshop.finance.entity.Budget;
import com.geekworkshop.finance.entity.Department;
import com.geekworkshop.finance.entity.IncomeRecord;
import com.geekworkshop.finance.entity.LaborApplication;
import com.geekworkshop.finance.entity.LaborCategory;
import com.geekworkshop.finance.entity.LaborRecipient;
import com.geekworkshop.finance.entity.LaborStatus;
import com.geekworkshop.finance.entity.PurchaseApplication;
import com.geekworkshop.finance.entity.PurchaseItem;
import com.geekworkshop.finance.entity.PurchaseStatus;
import com.geekworkshop.finance.entity.Reimbursement;
import com.geekworkshop.finance.entity.ReimbursementStatus;
import com.geekworkshop.finance.entity.UserRole;
import com.geekworkshop.finance.repository.AppUserRepository;
import com.geekworkshop.finance.repository.AdvanceApplicationRepository;
import com.geekworkshop.finance.repository.AssetAcceptanceRepository;
import com.geekworkshop.finance.repository.AssetHistoryRepository;
import com.geekworkshop.finance.repository.AssetRepository;
import com.geekworkshop.finance.repository.BudgetRepository;
import com.geekworkshop.finance.repository.DepartmentRepository;
import com.geekworkshop.finance.repository.IncomeRecordRepository;
import com.geekworkshop.finance.repository.LaborApplicationRepository;
import com.geekworkshop.finance.repository.PurchaseApplicationRepository;
import com.geekworkshop.finance.repository.ReimbursementRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

@Component
@Order(0)
public class DataInitializer implements CommandLineRunner {

    private final DepartmentRepository departmentRepository;
    private final AppUserRepository appUserRepository;
    private final BudgetRepository budgetRepository;
    private final ReimbursementRepository reimbursementRepository;
    private final PurchaseApplicationRepository purchaseApplicationRepository;
    private final LaborApplicationRepository laborApplicationRepository;
    private final AdvanceApplicationRepository advanceApplicationRepository;
    private final IncomeRecordRepository incomeRecordRepository;
    private final AssetAcceptanceRepository assetAcceptanceRepository;
    private final AssetRepository assetRepository;
    private final AssetHistoryRepository assetHistoryRepository;

    public DataInitializer(
            DepartmentRepository departmentRepository,
            AppUserRepository appUserRepository,
            BudgetRepository budgetRepository,
            ReimbursementRepository reimbursementRepository,
            PurchaseApplicationRepository purchaseApplicationRepository,
            LaborApplicationRepository laborApplicationRepository,
            AdvanceApplicationRepository advanceApplicationRepository,
            IncomeRecordRepository incomeRecordRepository,
            AssetAcceptanceRepository assetAcceptanceRepository,
            AssetRepository assetRepository,
            AssetHistoryRepository assetHistoryRepository
    ) {
        this.departmentRepository = departmentRepository;
        this.appUserRepository = appUserRepository;
        this.budgetRepository = budgetRepository;
        this.reimbursementRepository = reimbursementRepository;
        this.purchaseApplicationRepository = purchaseApplicationRepository;
        this.laborApplicationRepository = laborApplicationRepository;
        this.advanceApplicationRepository = advanceApplicationRepository;
        this.incomeRecordRepository = incomeRecordRepository;
        this.assetAcceptanceRepository = assetAcceptanceRepository;
        this.assetRepository = assetRepository;
        this.assetHistoryRepository = assetHistoryRepository;
    }

    @Override
    public void run(String... args) {
        Department research = ensureDepartment("RESEARCH", "Research Department", "Manager Wang");
        Department finance = ensureDepartment("FINANCE", "Finance Department", "Accountant Zhao");
        Department office = ensureDepartment("OFFICE", "General Office", "Office Director");
        Department leadership = ensureDepartment("LEADERSHIP", "Institute Leadership", "Executive Dean");

        AppUser employee = ensureUser("employee", "123456", "Student Zhang", UserRole.EMPLOYEE, research);
        ensureUser("manager", "123456", "Manager Wang", UserRole.DEPARTMENT_MANAGER, research);
        ensureUser("finance", "123456", "Accountant Zhao", UserRole.FINANCE, finance);
        ensureUser("office", "123456", "Office Chen", UserRole.OFFICE, office);
        AppUser executive = ensureUser("executive", "123456", "Executive Dean", UserRole.EXECUTIVE, leadership);
        ensureUser("cashier", "123456", "Cashier Liu", UserRole.CASHIER, finance);
        ensureUser("admin", "123456", "System Admin", UserRole.ADMIN, finance);

        ensureBudget(research, Year.now().getValue(), new BigDecimal("100000.00"));
        ensureBudget(finance, Year.now().getValue(), new BigDecimal("50000.00"));
        ensureDemoReimbursements(employee, research);
        ensureDemoPurchaseApplications(employee, finance, office);
        ensureDemoLaborApplications(employee, finance);
        ensureDemoAdvanceApplications(employee, finance);
        ensureDemoIncomeRecords(finance, executive);
        ensureDemoAssets(office, employee);
    }

    private Department ensureDepartment(String code, String name, String managerName) {
        return departmentRepository.findByCode(code).orElseGet(() -> {
            Department department = new Department();
            department.setCode(code);
            department.setName(name);
            department.setManagerName(managerName);
            return departmentRepository.save(department);
        });
    }

    private AppUser ensureUser(String username, String password, String realName, UserRole role, Department department) {
        return appUserRepository.findByUsername(username).orElseGet(() -> {
            AppUser user = new AppUser();
            user.setUsername(username);
            user.setPassword(password);
            user.setRealName(realName);
            user.setRole(role);
            user.setDepartment(department);
            user.setEnabled(true);
            return appUserRepository.save(user);
        });
    }

    private void ensureBudget(Department department, Integer year, BigDecimal amount) {
        budgetRepository.findByDepartmentIdAndBudgetYear(department.getId(), year).orElseGet(() -> {
            Budget budget = new Budget();
            budget.setDepartment(department);
            budget.setBudgetYear(year);
            budget.setTotalAmount(amount);
            budget.setUsedAmount(BigDecimal.ZERO);
            budget.setRemainingAmount(amount);
            return budgetRepository.save(budget);
        });
    }

    private void ensureDemoReimbursements(AppUser employee, Department department) {
        if (reimbursementRepository.count() > 0) {
            return;
        }

        createDemoReimbursement(employee, department, "Meeting taxi reimbursement", "Transport", "128.50", ReimbursementStatus.DRAFT, 2);
        createDemoReimbursement(employee, department, "Training hotel reimbursement", "Hotel", "680.00", ReimbursementStatus.SUBMITTED, 8);
        createDemoReimbursement(employee, department, "Office supplies purchase", "Office", "342.90", ReimbursementStatus.APPROVED, 15);
        createDemoReimbursement(employee, department, "Team lunch reimbursement", "Meal", "456.00", ReimbursementStatus.REJECTED, 24);
    }

    private void createDemoReimbursement(
            AppUser employee,
            Department department,
            String title,
            String expenseType,
            String amount,
            ReimbursementStatus status,
            int daysAgo
    ) {
        Reimbursement reimbursement = new Reimbursement();
        reimbursement.setApplicant(employee);
        reimbursement.setDepartment(department);
        reimbursement.setTitle(title);
        reimbursement.setExpenseType(expenseType);
        reimbursement.setAmount(new BigDecimal(amount));
        reimbursement.setExpenseDate(LocalDate.now().minusDays(daysAgo));
        reimbursement.setDescription("Demo data for presentation");
        reimbursement.setStatus(status);
        if (status != ReimbursementStatus.DRAFT) {
            reimbursement.setSubmittedAt(LocalDateTime.now().minusDays(daysAgo));
        }
        reimbursementRepository.save(reimbursement);
    }

    private void ensureDemoPurchaseApplications(AppUser applicant, Department finance, Department office) {
        if (purchaseApplicationRepository.count() > 0) {
            return;
        }

        PurchaseApplication application = new PurchaseApplication();
        application.setApplicant(applicant);
        application.setDepartment(office);
        application.setApplicationNumber("CG" + LocalDate.now().minusDays(3).toString().replace("-", "") + "001");
        application.setApplicantPhone("13800001111");
        application.setAmount(new BigDecimal("16800.00"));
        application.setBudgetNumber("BUD-RESEARCH-2026-01");
        application.setPurchaseMethod("询价采购");
        application.setTaxExempt(false);
        application.setUseLocation("科研楼 3 层实验室");
        application.setPurchaseReason("采购实验室服务器与显示器用于项目展示");
        application.setStatus(PurchaseStatus.COMPLETED);
        application.setSubmittedAt(LocalDateTime.now().minusDays(12));

        PurchaseItem item1 = new PurchaseItem();
        item1.setPurchaseApplication(application);
        item1.setItemName("显示器");
        item1.setSpecification("27英寸 2K");
        item1.setManufacturer("联想");
        item1.setUnitPrice(new BigDecimal("1299.00"));
        item1.setQuantity(4);
        item1.setTotalPrice(new BigDecimal("5196.00"));

        PurchaseItem item2 = new PurchaseItem();
        item2.setPurchaseApplication(application);
        item2.setItemName("工作站主机");
        item2.setSpecification("i7/32G/1T SSD");
        item2.setManufacturer("戴尔");
        item2.setUnitPrice(new BigDecimal("5801.00"));
        item2.setQuantity(2);
        item2.setTotalPrice(new BigDecimal("11602.00"));

        application.setItems(new ArrayList<>(List.of(item1, item2)));
        purchaseApplicationRepository.save(application);
    }

    private void ensureDemoLaborApplications(AppUser applicant, Department finance) {
        if (laborApplicationRepository.count() > 0) {
            return;
        }

        LaborApplication application = new LaborApplication();
        application.setApplicant(applicant);
        application.setDepartment(finance);
        application.setApplicationNumber("LW" + LocalDate.now().minusDays(5).toString().replace("-", "") + "001");
        application.setCategory(LaborCategory.RESEARCH_LABOR);
        application.setTitle("科研劳务费发放");
        application.setDescription("用于项目组阶段性成果整理与资料录入");
        application.setBudgetNumber("BUD-FIN-2026-02");
        application.setTotalAmount(new BigDecimal("3200.00"));
        application.setAmountInWords("叁仟贰佰元整");
        application.setStatus(LaborStatus.COMPLETED);
        application.setSubmittedAt(LocalDateTime.now().minusDays(9));
        application.setPaymentDate(LocalDate.now().minusDays(2));
        application.setPaymentAmount(new BigDecimal("3200.00"));
        application.setPaymentVoucherNumber("PV20260624001");

        LaborRecipient recipient = new LaborRecipient();
        recipient.setLaborApplication(application);
        recipient.setName("李明");
        recipient.setPhone("13900002222");
        recipient.setIdCard("320101199012123456");
        recipient.setOrganization("外聘专家");
        recipient.setPosition("讲师");
        recipient.setServiceContent("项目资料校对与讲解");
        recipient.setNetAmount(new BigDecimal("3200.00"));
        recipient.setBankAccount("6222021234567890123");
        recipient.setBankName("中国工商银行");

        application.setRecipients(new ArrayList<>(List.of(recipient)));
        laborApplicationRepository.save(application);
    }

    private void ensureDemoAdvanceApplications(AppUser applicant, Department finance) {
        if (advanceApplicationRepository.count() > 0) {
            return;
        }

        AdvanceApplication temporaryLoan = new AdvanceApplication();
        temporaryLoan.setApplicant(applicant);
        temporaryLoan.setDepartment(finance);
        temporaryLoan.setApplicationNumber("YF" + LocalDate.now().minusDays(8).toString().replace("-", "") + "001");
        temporaryLoan.setType(AdvanceType.TEMPORARY_LOAN);
        temporaryLoan.setReason("差旅临时借款");
        temporaryLoan.setAmount(new BigDecimal("5000.00"));
        temporaryLoan.setPaymentMethod("银行转账");
        temporaryLoan.setPayeeName("张三");
        temporaryLoan.setBankAccount("6222020000111122222");
        temporaryLoan.setBankName("中国建设银行");
        temporaryLoan.setExpectedRepaymentDate(LocalDate.now().plusDays(15));
        temporaryLoan.setStatus(AdvanceStatus.PAID);
        temporaryLoan.setSettlementStatus(com.geekworkshop.finance.entity.SettlementStatus.PENDING_OFFSET);
        temporaryLoan.setOffsetAmount(BigDecimal.ZERO);
        temporaryLoan.setSubmittedAt(LocalDateTime.now().minusDays(7));
        temporaryLoan.setPaymentDate(LocalDate.now().minusDays(6));
        temporaryLoan.setPaymentAmount(new BigDecimal("5000.00"));
        temporaryLoan.setPaymentVoucherNumber("YF20260622001");
        advanceApplicationRepository.save(temporaryLoan);

        AdvanceApplication prepayment = new AdvanceApplication();
        prepayment.setApplicant(applicant);
        prepayment.setDepartment(finance);
        prepayment.setApplicationNumber("YF" + LocalDate.now().minusDays(10).toString().replace("-", "") + "002");
        prepayment.setType(AdvanceType.PREPAYMENT);
        prepayment.setReason("合作方软件服务预付款");
        prepayment.setAmount(new BigDecimal("18000.00"));
        prepayment.setPaymentMethod("对公转账");
        prepayment.setPayeeName("北京某科技有限公司");
        prepayment.setBankAccount("1101010012345678901");
        prepayment.setBankName("中国银行");
        prepayment.setPartnerName("北京某科技有限公司");
        prepayment.setExpectedSettlementDate(LocalDate.now().plusDays(30));
        prepayment.setStatus(AdvanceStatus.COMPLETED);
        prepayment.setSettlementStatus(com.geekworkshop.finance.entity.SettlementStatus.OFFSET_COMPLETED);
        prepayment.setOffsetAmount(new BigDecimal("18000.00"));
        prepayment.setSubmittedAt(LocalDateTime.now().minusDays(9));
        prepayment.setPaymentDate(LocalDate.now().minusDays(8));
        prepayment.setPaymentAmount(new BigDecimal("18000.00"));
        prepayment.setPaymentVoucherNumber("YF20260620002");
        advanceApplicationRepository.save(prepayment);
    }

    private void ensureDemoIncomeRecords(Department finance, AppUser createdBy) {
        if (incomeRecordRepository.count() > 0) {
            return;
        }

        IncomeRecord first = new IncomeRecord();
        first.setIncomeNumber("SR" + LocalDate.now().minusDays(4).toString().replace("-", "") + "001");
        first.setReceiptDate(LocalDate.now().minusDays(4));
        first.setVoucherNumber("SK20260626001");
        first.setPayerName("合作单位A");
        first.setIncomeCategory("项目到账");
        first.setAmount(new BigDecimal("50000.00"));
        first.setFundingSource("横向课题");
        first.setArrivalAccount("基本户");
        first.setInvoiceStatus("已开票");
        first.setRemark("项目首笔到账");
        first.setDepartment(finance);
        first.setCreatedBy(createdBy);
        incomeRecordRepository.save(first);

        IncomeRecord second = new IncomeRecord();
        second.setIncomeNumber("SR" + LocalDate.now().minusDays(2).toString().replace("-", "") + "002");
        second.setReceiptDate(LocalDate.now().minusDays(2));
        second.setVoucherNumber("SK20260628002");
        second.setPayerName("研究生创新基金");
        second.setIncomeCategory("其他收入");
        second.setAmount(new BigDecimal("12000.00"));
        second.setFundingSource("校内拨款");
        second.setArrivalAccount("财务专户");
        second.setInvoiceStatus("未开票");
        second.setRemark("培训材料收入");
        second.setDepartment(finance);
        second.setCreatedBy(createdBy);
        incomeRecordRepository.save(second);
    }

    private void ensureDemoAssets(Department office, AppUser officeUser) {
        if (assetAcceptanceRepository.count() > 0 || assetRepository.count() > 0) {
            return;
        }

        PurchaseApplication purchase = purchaseApplicationRepository.findAll().stream().findFirst().orElse(null);
        if (purchase == null) {
            return;
        }
        PurchaseApplication detail = purchaseApplicationRepository.findDetailById(purchase.getId()).orElse(null);
        if (detail == null || detail.getItems().isEmpty()) {
            return;
        }

        AssetAcceptance acceptance = new AssetAcceptance();
        acceptance.setAcceptanceNumber("YS" + LocalDate.now().minusDays(11).toString().replace("-", "") + "001");
        acceptance.setPurchaseApplication(detail);
        acceptance.setAcceptedBy(officeUser);
        acceptance.setReceivedAt(LocalDateTime.now().minusDays(11));
        acceptance.setStorageLocation("办公室设备库");
        acceptance.setRemark("验收入库");
        acceptance = assetAcceptanceRepository.save(acceptance);

        PurchaseItem item = detail.getItems().get(0);
        Asset asset = new Asset();
        asset.setAssetNumber("ZC" + LocalDate.now().minusDays(11).toString().replace("-", "") + "001");
        asset.setAcceptance(acceptance);
        asset.setPurchaseItem(item);
        asset.setItemName(item.getItemName());
        asset.setSpecification(item.getSpecification());
        asset.setManufacturer(item.getManufacturer());
        asset.setQuantity(item.getQuantity());
        asset.setTotalPrice(item.getTotalPrice());
        asset.setReceivedAt(LocalDateTime.now().minusDays(11));
        asset.setLocation("科研楼 3 层实验室");
        asset.setStatus(AssetStatus.IN_USE);
        asset.setCustodian(officeUser);
        asset = assetRepository.save(asset);

        AssetHistory inbound = new AssetHistory();
        inbound.setAsset(asset);
        inbound.setReceiptNumber(acceptance.getAcceptanceNumber());
        inbound.setAction(AssetHistoryAction.ACCEPTED_INBOUND);
        inbound.setOperator(officeUser);
        inbound.setCustodian(officeUser);
        inbound.setLocation("科研楼 3 层实验室");
        inbound.setAssetStatus(AssetStatus.IN_USE);
        inbound.setRemark("验收入库并交付使用");
        assetHistoryRepository.save(inbound);
    }
}
