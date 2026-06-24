package com.geekworkshop.finance.dto;

import com.geekworkshop.finance.entity.AppUser;
import com.geekworkshop.finance.entity.UserRole;

import java.time.LocalDateTime;

public class UserResponse {

    private Long id;
    private String username;
    private String realName;
    private UserRole role;
    private Long departmentId;
    private String departmentName;
    private String phone;
    private String email;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserResponse fromEntity(AppUser user) {
        UserResponse response = new UserResponse();
        response.id = user.getId();
        response.username = user.getUsername();
        response.realName = user.getRealName();
        response.role = user.getRole();
        if (user.getDepartment() != null) {
            response.departmentId = user.getDepartment().getId();
            response.departmentName = user.getDepartment().getName();
        }
        response.phone = user.getPhone();
        response.email = user.getEmail();
        response.enabled = user.getEnabled();
        response.createdAt = user.getCreatedAt();
        response.updatedAt = user.getUpdatedAt();
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

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
