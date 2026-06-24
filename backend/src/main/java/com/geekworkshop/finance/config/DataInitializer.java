package com.geekworkshop.finance.config;

import com.geekworkshop.finance.entity.AppUser;
import com.geekworkshop.finance.entity.Budget;
import com.geekworkshop.finance.entity.Department;
import com.geekworkshop.finance.entity.Reimbursement;
import com.geekworkshop.finance.entity.ReimbursementStatus;
import com.geekworkshop.finance.entity.UserRole;
import com.geekworkshop.finance.repository.AppUserRepository;
import com.geekworkshop.finance.repository.BudgetRepository;
import com.geekworkshop.finance.repository.DepartmentRepository;
import com.geekworkshop.finance.repository.ReimbursementRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;

@Component
public class DataInitializer implements CommandLineRunner {

    private final DepartmentRepository departmentRepository;
    private final AppUserRepository appUserRepository;
    private final BudgetRepository budgetRepository;
    private final ReimbursementRepository reimbursementRepository;

    public DataInitializer(
            DepartmentRepository departmentRepository,
            AppUserRepository appUserRepository,
            BudgetRepository budgetRepository,
            ReimbursementRepository reimbursementRepository
    ) {
        this.departmentRepository = departmentRepository;
        this.appUserRepository = appUserRepository;
        this.budgetRepository = budgetRepository;
        this.reimbursementRepository = reimbursementRepository;
    }

    @Override
    public void run(String... args) {
        Department research = ensureDepartment("RESEARCH", "Research Department", "Manager Wang");
        Department finance = ensureDepartment("FINANCE", "Finance Department", "Accountant Zhao");

        AppUser employee = ensureUser("employee", "123456", "Student Zhang", UserRole.EMPLOYEE, research);
        ensureUser("manager", "123456", "Manager Wang", UserRole.DEPARTMENT_MANAGER, research);
        ensureUser("finance", "123456", "Accountant Zhao", UserRole.FINANCE, finance);
        ensureUser("admin", "123456", "System Admin", UserRole.ADMIN, finance);

        ensureBudget(research, Year.now().getValue(), new BigDecimal("100000.00"));
        ensureBudget(finance, Year.now().getValue(), new BigDecimal("50000.00"));
        ensureDemoReimbursements(employee, research);
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
}
