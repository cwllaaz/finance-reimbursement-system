package com.geekworkshop.finance.dto;

import com.geekworkshop.finance.entity.UserRole;
import java.time.LocalDateTime;

public record LaborTimelineResponse(
        String title, String status, String operatorName, UserRole operatorRole,
        String comment, LocalDateTime time, String nodeType
) {}
