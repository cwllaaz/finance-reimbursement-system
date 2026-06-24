package com.geekworkshop.finance.dto;

import com.geekworkshop.finance.entity.Department;

public class DepartmentResponse {

    private Long id;
    private String name;
    private String code;
    private String managerName;

    public static DepartmentResponse fromEntity(Department department) {
        DepartmentResponse response = new DepartmentResponse();
        response.id = department.getId();
        response.name = department.getName();
        response.code = department.getCode();
        response.managerName = department.getManagerName();
        return response;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public String getManagerName() {
        return managerName;
    }
}
