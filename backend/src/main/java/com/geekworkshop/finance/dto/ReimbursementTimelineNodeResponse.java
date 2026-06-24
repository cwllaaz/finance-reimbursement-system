package com.geekworkshop.finance.dto;

import com.geekworkshop.finance.entity.UserRole;

import java.time.LocalDateTime;

public class ReimbursementTimelineNodeResponse {

    private String title;
    private String status;
    private String operatorName;
    private UserRole operatorRole;
    private String comment;
    private LocalDateTime time;
    private String nodeType;

    public ReimbursementTimelineNodeResponse(
            String title,
            String status,
            String operatorName,
            UserRole operatorRole,
            String comment,
            LocalDateTime time,
            String nodeType
    ) {
        this.title = title;
        this.status = status;
        this.operatorName = operatorName;
        this.operatorRole = operatorRole;
        this.comment = comment;
        this.time = time;
        this.nodeType = nodeType;
    }

    public String getTitle() {
        return title;
    }

    public String getStatus() {
        return status;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public UserRole getOperatorRole() {
        return operatorRole;
    }

    public String getComment() {
        return comment;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public String getNodeType() {
        return nodeType;
    }
}
