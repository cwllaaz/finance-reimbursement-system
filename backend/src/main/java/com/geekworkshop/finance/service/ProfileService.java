package com.geekworkshop.finance.service;

import com.geekworkshop.finance.dto.PasswordChangeRequest;
import com.geekworkshop.finance.dto.ProfileRequest;
import com.geekworkshop.finance.dto.UserResponse;
import com.geekworkshop.finance.entity.AppUser;
import com.geekworkshop.finance.exception.BusinessException;
import com.geekworkshop.finance.repository.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProfileService {

    private final AppUserRepository appUserRepository;
    private final OperationLogService operationLogService;

    public ProfileService(AppUserRepository appUserRepository, OperationLogService operationLogService) {
        this.appUserRepository = appUserRepository;
        this.operationLogService = operationLogService;
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(AppUser currentUser) {
        return UserResponse.fromEntity(findCurrentUser(currentUser));
    }

    @Transactional
    public UserResponse updateProfile(AppUser currentUser, ProfileRequest request) {
        AppUser user = findCurrentUser(currentUser);
        user.setRealName(normalizeRequired(request.getRealName(), "请输入真实姓名"));
        user.setPhone(normalizeOptional(request.getPhone()));
        user.setEmail(normalizeOptional(request.getEmail()));
        AppUser saved = appUserRepository.save(user);
        operationLogService.record(saved, "个人资料", "修改个人资料", saved.getId(), saved.getRealName(), "更新姓名、手机号或邮箱");
        return UserResponse.fromEntity(saved);
    }

    @Transactional
    public void changePassword(AppUser currentUser, PasswordChangeRequest request) {
        AppUser user = findCurrentUser(currentUser);
        String oldPassword = request.getOldPassword() == null ? "" : request.getOldPassword().trim();
        String newPassword = request.getNewPassword() == null ? "" : request.getNewPassword().trim();

        if (!user.getPassword().equals(oldPassword)) {
            throw new BusinessException("原密码不正确");
        }
        if (user.getPassword().equals(newPassword)) {
            throw new BusinessException("新密码不能和原密码相同");
        }

        user.setPassword(newPassword);
        appUserRepository.save(user);
        operationLogService.record(user, "个人资料", "修改密码", user.getId(), user.getUsername(), "当前用户修改登录密码");
    }

    private AppUser findCurrentUser(AppUser currentUser) {
        return appUserRepository.findWithDepartmentById(currentUser.getId())
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }

    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
