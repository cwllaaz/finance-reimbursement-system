package com.geekworkshop.finance.controller;

import com.geekworkshop.finance.dto.WorkbenchItemResponse;
import com.geekworkshop.finance.entity.AppUser;
import com.geekworkshop.finance.entity.WorkbenchScope;
import com.geekworkshop.finance.service.AuthService;
import com.geekworkshop.finance.service.WorkbenchService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workbench")
public class WorkbenchController {
    private final WorkbenchService workbenchService;
    private final AuthService authService;

    public WorkbenchController(WorkbenchService workbenchService, AuthService authService) {
        this.workbenchService = workbenchService;
        this.authService = authService;
    }

    @GetMapping("/{scope}")
    public List<WorkbenchItemResponse> list(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable WorkbenchScope scope,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword
    ) {
        AppUser user = authService.requireUser(token);
        return workbenchService.list(user, scope, businessType, status, keyword);
    }
}
