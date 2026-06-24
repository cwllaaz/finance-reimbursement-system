package com.geekworkshop.finance.service;

import com.geekworkshop.finance.dto.LoginRequest;
import com.geekworkshop.finance.dto.LoginResponse;
import com.geekworkshop.finance.dto.UserInfoResponse;
import com.geekworkshop.finance.entity.AppUser;
import com.geekworkshop.finance.exception.BusinessException;
import com.geekworkshop.finance.repository.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final OperationLogService operationLogService;
    private final Map<String, Long> tokenStore = new ConcurrentHashMap<>();

    public AuthService(AppUserRepository appUserRepository, OperationLogService operationLogService) {
        this.appUserRepository = appUserRepository;
        this.operationLogService = operationLogService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        AppUser user = appUserRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("username or password is incorrect"));

        if (!Boolean.TRUE.equals(user.getEnabled()) || !user.getPassword().equals(request.getPassword())) {
            throw new BusinessException("username or password is incorrect");
        }

        String token = UUID.randomUUID().toString();
        tokenStore.put(token, user.getId());
        operationLogService.record(user, "认证", "登录成功", user.getId(), user.getUsername(), "用户登录系统");
        return new LoginResponse(token, UserInfoResponse.fromEntity(user));
    }

    @Transactional(readOnly = true)
    public AppUser requireUser(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException("please login first");
        }

        Long userId = tokenStore.get(token);
        if (userId == null) {
            throw new BusinessException("login status has expired");
        }

        return appUserRepository.findWithDepartmentById(userId)
                .orElseThrow(() -> new BusinessException("user not found"));
    }

    public void logout(String token) {
        if (token != null) {
            tokenStore.remove(token);
        }
    }
}
