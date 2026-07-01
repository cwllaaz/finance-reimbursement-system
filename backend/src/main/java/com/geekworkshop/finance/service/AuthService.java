package com.geekworkshop.finance.service;

import com.geekworkshop.finance.dto.LoginRequest;
import com.geekworkshop.finance.dto.LoginResponse;
import com.geekworkshop.finance.dto.UserInfoResponse;
import com.geekworkshop.finance.entity.AppUser;
import com.geekworkshop.finance.exception.BusinessException;
import com.geekworkshop.finance.repository.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final OperationLogService operationLogService;
    private final PasswordService passwordService;
    private final Map<String, TokenSession> tokenStore = new ConcurrentHashMap<>();

    @Value("${app.auth-token-ttl-minutes:120}")
    private long tokenTtlMinutes;

    public AuthService(
            AppUserRepository appUserRepository,
            OperationLogService operationLogService,
            PasswordService passwordService
    ) {
        this.appUserRepository = appUserRepository;
        this.operationLogService = operationLogService;
        this.passwordService = passwordService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        AppUser user = appUserRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("username or password is incorrect"));

        if (!Boolean.TRUE.equals(user.getEnabled())
                || !passwordService.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("username or password is incorrect");
        }

        String token = UUID.randomUUID().toString();
        tokenStore.put(token, new TokenSession(
                user.getId(),
                Instant.now().plusSeconds(Math.max(0, tokenTtlMinutes) * 60)
        ));
        operationLogService.record(user, "认证", "登录成功", user.getId(), user.getUsername(), "用户登录系统");
        return new LoginResponse(token, UserInfoResponse.fromEntity(user));
    }

    @Transactional(readOnly = true)
    public AppUser requireUser(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException("please login first");
        }

        TokenSession session = tokenStore.get(token);
        if (session == null) {
            throw new BusinessException("login status has expired");
        }
        if (session.expiresAt().isBefore(Instant.now())) {
            tokenStore.remove(token);
            throw new BusinessException("login status has expired");
        }

        return appUserRepository.findWithDepartmentById(session.userId())
                .orElseThrow(() -> new BusinessException("user not found"));
    }

    public void logout(String token) {
        if (token != null) {
            tokenStore.remove(token);
        }
    }

    public void invalidateUserSessions(Long userId) {
        tokenStore.entrySet().removeIf(entry -> entry.getValue().userId().equals(userId));
    }

    private record TokenSession(Long userId, Instant expiresAt) {}
}
