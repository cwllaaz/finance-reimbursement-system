package com.geekworkshop.finance.dto;

import com.geekworkshop.finance.entity.OperationLog;
import com.geekworkshop.finance.entity.UserRole;

import java.time.LocalDateTime;

public class OperationLogResponse {

    private Long id;
    private Long userId;
    private String username;
    private String realName;
    private UserRole role;
    private String action;
    private String module;
    private Long targetId;
    private String targetName;
    private String detail;
    private String ipAddress;
    private LocalDateTime createdAt;

    public static OperationLogResponse fromEntity(OperationLog log) {
        OperationLogResponse response = new OperationLogResponse();
        response.id = log.getId();
        response.userId = log.getUserId();
        response.username = log.getUsername();
        response.realName = log.getRealName();
        response.role = log.getRole();
        response.action = log.getAction();
        response.module = log.getModule();
        response.targetId = log.getTargetId();
        response.targetName = log.getTargetName();
        response.detail = log.getDetail();
        response.ipAddress = log.getIpAddress();
        response.createdAt = log.getCreatedAt();
        return response;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
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

    public String getAction() {
        return action;
    }

    public String getModule() {
        return module;
    }

    public Long getTargetId() {
        return targetId;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getDetail() {
        return detail;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
