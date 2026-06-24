package com.geekworkshop.finance.service;

import com.geekworkshop.finance.dto.OperationLogResponse;
import com.geekworkshop.finance.entity.AppUser;
import com.geekworkshop.finance.entity.OperationLog;
import com.geekworkshop.finance.entity.UserRole;
import com.geekworkshop.finance.exception.BusinessException;
import com.geekworkshop.finance.repository.OperationLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

@Service
public class OperationLogService {

    private final OperationLogRepository operationLogRepository;

    public OperationLogService(OperationLogRepository operationLogRepository) {
        this.operationLogRepository = operationLogRepository;
    }

    @Transactional
    public void record(
            AppUser user,
            String module,
            String action,
            Long targetId,
            String targetName,
            String detail
    ) {
        OperationLog log = new OperationLog();
        if (user != null) {
            log.setUserId(user.getId());
            log.setUsername(user.getUsername());
            log.setRealName(user.getRealName());
            log.setRole(user.getRole());
        }
        log.setModule(module);
        log.setAction(action);
        log.setTargetId(targetId);
        log.setTargetName(truncate(targetName, 160));
        log.setDetail(truncate(detail, 1000));
        log.setIpAddress(currentIpAddress());
        operationLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<OperationLogResponse> list(AppUser currentUser, String keyword, String module, String action) {
        if (currentUser.getRole() != UserRole.ADMIN) {
            throw new BusinessException("只有管理员可以查看操作日志");
        }

        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        String normalizedModule = StringUtils.hasText(module) ? module.trim() : null;
        String normalizedAction = StringUtils.hasText(action) ? action.trim() : null;

        return operationLogRepository.search(normalizedKeyword, normalizedModule, normalizedAction)
                .stream()
                .map(OperationLogResponse::fromEntity)
                .toList();
    }

    private String currentIpAddress() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }

        HttpServletRequest request = attributes.getRequest();
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
