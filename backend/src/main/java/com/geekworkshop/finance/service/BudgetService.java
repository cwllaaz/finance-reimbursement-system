package com.geekworkshop.finance.service;

import com.geekworkshop.finance.dto.BudgetRequest;
import com.geekworkshop.finance.dto.BudgetResponse;
import com.geekworkshop.finance.entity.AppUser;
import com.geekworkshop.finance.entity.Budget;
import com.geekworkshop.finance.entity.UserRole;
import com.geekworkshop.finance.exception.BusinessException;
import com.geekworkshop.finance.repository.BudgetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final OperationLogService operationLogService;

    public BudgetService(BudgetRepository budgetRepository, OperationLogService operationLogService) {
        this.budgetRepository = budgetRepository;
        this.operationLogService = operationLogService;
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> list(AppUser currentUser) {
        if (currentUser.getRole() != UserRole.FINANCE && currentUser.getRole() != UserRole.ADMIN) {
            throw new BusinessException("no permission to view budgets");
        }

        return budgetRepository.findAllWithDepartment()
                .stream()
                .map(BudgetResponse::fromEntity)
                .toList();
    }

    @Transactional
    public BudgetResponse update(AppUser currentUser, Long id, BudgetRequest request) {
        if (currentUser.getRole() != UserRole.FINANCE && currentUser.getRole() != UserRole.ADMIN) {
            throw new BusinessException("no permission to update budgets");
        }

        Budget budget = budgetRepository.findDetailById(id)
                .orElseThrow(() -> new BusinessException("budget not found"));

        BigDecimal totalAmount = request.getTotalAmount();
        if (totalAmount.compareTo(budget.getUsedAmount()) < 0) {
            throw new BusinessException("total budget cannot be less than used amount");
        }

        budget.setTotalAmount(totalAmount);
        budget.setRemainingAmount(totalAmount.subtract(budget.getUsedAmount()));
        Budget saved = budgetRepository.save(budget);
        operationLogService.record(
                currentUser,
                "预算管理",
                "修改预算",
                saved.getId(),
                saved.getDepartment() == null ? null : saved.getDepartment().getName(),
                "设置总预算为 " + totalAmount
        );
        return BudgetResponse.fromEntity(saved);
    }
}
