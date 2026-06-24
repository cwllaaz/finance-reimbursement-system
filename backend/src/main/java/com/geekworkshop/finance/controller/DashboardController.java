package com.geekworkshop.finance.controller;

import com.geekworkshop.finance.dto.DashboardStatsResponse;
import com.geekworkshop.finance.entity.AppUser;
import com.geekworkshop.finance.service.AuthService;
import com.geekworkshop.finance.service.ReimbursementService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final ReimbursementService reimbursementService;
    private final AuthService authService;

    public DashboardController(ReimbursementService reimbursementService, AuthService authService) {
        this.reimbursementService = reimbursementService;
        this.authService = authService;
    }

    @GetMapping("/stats")
    public DashboardStatsResponse stats(@RequestHeader("X-Auth-Token") String token) {
        AppUser currentUser = authService.requireUser(token);
        return reimbursementService.dashboardStats(currentUser);
    }
}
