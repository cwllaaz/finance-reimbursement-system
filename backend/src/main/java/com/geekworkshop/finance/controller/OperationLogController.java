package com.geekworkshop.finance.controller;

import com.geekworkshop.finance.dto.OperationLogResponse;
import com.geekworkshop.finance.entity.AppUser;
import com.geekworkshop.finance.service.AuthService;
import com.geekworkshop.finance.service.OperationLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/operation-logs")
public class OperationLogController {

    private final OperationLogService operationLogService;
    private final AuthService authService;

    public OperationLogController(OperationLogService operationLogService, AuthService authService) {
        this.operationLogService = operationLogService;
        this.authService = authService;
    }

    @GetMapping
    public List<OperationLogResponse> list(
            @RequestHeader("X-Auth-Token") String token,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action
    ) {
        AppUser currentUser = authService.requireUser(token);
        return operationLogService.list(currentUser, keyword, module, action);
    }
}
