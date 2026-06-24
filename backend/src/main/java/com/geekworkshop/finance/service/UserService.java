package com.geekworkshop.finance.service;

import com.geekworkshop.finance.dto.UserRequest;
import com.geekworkshop.finance.dto.UserResponse;
import com.geekworkshop.finance.entity.AppUser;
import com.geekworkshop.finance.entity.Department;
import com.geekworkshop.finance.entity.UserRole;
import com.geekworkshop.finance.exception.BusinessException;
import com.geekworkshop.finance.repository.AppUserRepository;
import com.geekworkshop.finance.repository.DepartmentRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class UserService {

    private final AppUserRepository appUserRepository;
    private final DepartmentRepository departmentRepository;
    private final OperationLogService operationLogService;

    public UserService(
            AppUserRepository appUserRepository,
            DepartmentRepository departmentRepository,
            OperationLogService operationLogService
    ) {
        this.appUserRepository = appUserRepository;
        this.departmentRepository = departmentRepository;
        this.operationLogService = operationLogService;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list(AppUser currentUser, String keyword, UserRole role) {
        assertAdmin(currentUser);
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase() : null;
        return appUserRepository.findAllWithDepartment()
                .stream()
                .filter(user -> role == null || user.getRole() == role)
                .filter(user -> matchesKeyword(user, normalizedKeyword))
                .map(UserResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getById(AppUser currentUser, Long id) {
        assertAdmin(currentUser);
        return UserResponse.fromEntity(findUser(id));
    }

    @Transactional
    public UserResponse create(AppUser currentUser, UserRequest request) {
        assertAdmin(currentUser);
        if (!StringUtils.hasText(request.getPassword())) {
            throw new BusinessException("请输入初始密码");
        }

        String username = normalizeRequired(request.getUsername(), "请输入用户名");
        if (appUserRepository.existsByUsername(username)) {
            throw new BusinessException("用户名已存在");
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPassword(request.getPassword().trim());
        fillUser(user, request);
        AppUser saved = appUserRepository.save(user);
        operationLogService.record(currentUser, "用户管理", "新增用户", saved.getId(), saved.getUsername(), "新增账号：" + saved.getRealName());
        return UserResponse.fromEntity(saved);
    }

    @Transactional
    public UserResponse update(AppUser currentUser, Long id, UserRequest request) {
        assertAdmin(currentUser);
        AppUser user = findUser(id);

        String username = normalizeRequired(request.getUsername(), "请输入用户名");
        if (!user.getUsername().equals(username) && appUserRepository.existsByUsername(username)) {
            throw new BusinessException("用户名已存在");
        }

        user.setUsername(username);
        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(request.getPassword().trim());
        }
        fillUser(user, request);
        AppUser saved = appUserRepository.save(user);
        operationLogService.record(currentUser, "用户管理", "编辑用户", saved.getId(), saved.getUsername(), "编辑账号资料：" + saved.getRealName());
        return UserResponse.fromEntity(saved);
    }

    @Transactional
    public UserResponse updateStatus(AppUser currentUser, Long id, Boolean enabled) {
        assertAdmin(currentUser);
        if (enabled == null) {
            throw new BusinessException("请选择账号状态");
        }
        if (currentUser.getId().equals(id) && !enabled) {
            throw new BusinessException("不能禁用当前登录的管理员账号");
        }

        AppUser user = findUser(id);
        user.setEnabled(enabled);
        AppUser saved = appUserRepository.save(user);
        operationLogService.record(
                currentUser,
                "用户管理",
                enabled ? "启用用户" : "禁用用户",
                saved.getId(),
                saved.getUsername(),
                (enabled ? "启用账号：" : "禁用账号：") + saved.getRealName()
        );
        return UserResponse.fromEntity(saved);
    }

    @Transactional
    public void delete(AppUser currentUser, Long id) {
        assertAdmin(currentUser);
        if (currentUser.getId().equals(id)) {
            throw new BusinessException("不能删除当前登录的管理员账号");
        }

        AppUser user = findUser(id);
        String targetName = user.getUsername();
        String detail = "删除账号：" + user.getRealName();
        try {
            appUserRepository.delete(user);
            appUserRepository.flush();
            operationLogService.record(currentUser, "用户管理", "删除用户", id, targetName, detail);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException("该用户已有业务数据，建议改为禁用账号");
        }
    }

    private void fillUser(AppUser user, UserRequest request) {
        user.setRealName(normalizeRequired(request.getRealName(), "请输入真实姓名"));
        user.setRole(request.getRole());
        user.setDepartment(resolveDepartment(request.getDepartmentId()));
        user.setPhone(normalizeOptional(request.getPhone()));
        user.setEmail(normalizeOptional(request.getEmail()));
        user.setEnabled(request.getEnabled() == null ? Boolean.TRUE : request.getEnabled());
    }

    private AppUser findUser(Long id) {
        return appUserRepository.findWithDepartmentById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }

    private Department resolveDepartment(Long departmentId) {
        if (departmentId == null) {
            return null;
        }
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new BusinessException("部门不存在"));
    }

    private boolean matchesKeyword(AppUser user, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        return contains(user.getUsername(), keyword)
                || contains(user.getRealName(), keyword)
                || contains(user.getPhone(), keyword)
                || contains(user.getEmail(), keyword)
                || (user.getDepartment() != null && contains(user.getDepartment().getName(), keyword));
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private void assertAdmin(AppUser currentUser) {
        if (currentUser.getRole() != UserRole.ADMIN) {
            throw new BusinessException("只有管理员可以访问用户管理");
        }
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
