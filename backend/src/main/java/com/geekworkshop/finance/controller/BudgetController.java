package com.geekworkshop.finance.controller;

import com.geekworkshop.finance.dto.BudgetRequest;
import com.geekworkshop.finance.dto.BudgetResponse;
import com.geekworkshop.finance.entity.AppUser;
import com.geekworkshop.finance.service.AuthService;
import com.geekworkshop.finance.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;
    private final AuthService authService;

    public BudgetController(BudgetService budgetService, AuthService authService) {
        this.budgetService = budgetService;
        this.authService = authService;
    }

    @GetMapping
    public List<BudgetResponse> list(@RequestHeader("X-Auth-Token") String token) {
        AppUser currentUser = authService.requireUser(token);
        return budgetService.list(currentUser);
    }

    @PutMapping("/{id}")
    public BudgetResponse update(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long id,
            @Valid @RequestBody BudgetRequest request
    ) {
        AppUser currentUser = authService.requireUser(token);
        return budgetService.update(currentUser, id, request);
    }
}
