package com.geekworkshop.finance.controller;

import com.geekworkshop.finance.dto.DepartmentResponse;
import com.geekworkshop.finance.entity.Department;
import com.geekworkshop.finance.repository.DepartmentRepository;
import com.geekworkshop.finance.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentRepository departmentRepository;
    private final AuthService authService;

    public DepartmentController(DepartmentRepository departmentRepository, AuthService authService) {
        this.departmentRepository = departmentRepository;
        this.authService = authService;
    }

    @GetMapping
    public List<DepartmentResponse> list(@RequestHeader("X-Auth-Token") String token) {
        authService.requireUser(token);
        return departmentRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Department::getId))
                .map(DepartmentResponse::fromEntity)
                .toList();
    }
}
