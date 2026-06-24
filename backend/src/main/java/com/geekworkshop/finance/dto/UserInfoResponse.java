package com.geekworkshop.finance.dto;

import com.geekworkshop.finance.entity.AppUser;
import com.geekworkshop.finance.entity.UserRole;

public class UserInfoResponse {

    private Long id;
    private String username;
    private String realName;
    private UserRole role;
    private Long departmentId;
    private String departmentName;

    public static UserInfoResponse fromEntity(AppUser user) {
        UserInfoResponse response = new UserInfoResponse();
        response.id = user.getId();
        response.username = user.getUsername();
        response.realName = user.getRealName();
        response.role = user.getRole();

        if (user.getDepartment() != null) {
            response.departmentId = user.getDepartment().getId();
            response.departmentName = user.getDepartment().getName();
        }

        return response;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getRealName() {
        return realName;
    }

    public UserRole getRole() {
        return role;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }
}
