package com.geekworkshop.finance.controller;

import com.geekworkshop.finance.dto.UserRequest;
import com.geekworkshop.finance.dto.UserResponse;
import com.geekworkshop.finance.entity.AppUser;
import com.geekworkshop.finance.entity.UserRole;
import com.geekworkshop.finance.exception.BusinessException;
import com.geekworkshop.finance.service.AuthService;
import com.geekworkshop.finance.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    public UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @GetMapping
    public List<UserResponse> list(
            @RequestHeader("X-Auth-Token") String token,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserRole role
    ) {
        AppUser currentUser = authService.requireUser(token);
        return userService.list(currentUser, keyword, role);
    }

    @GetMapping("/{id}")
    public UserResponse getById(@RequestHeader("X-Auth-Token") String token, @PathVariable Long id) {
        AppUser currentUser = authService.requireUser(token);
        return userService.getById(currentUser, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(
            @RequestHeader("X-Auth-Token") String token,
            @Valid @RequestBody UserRequest request
    ) {
        AppUser currentUser = authService.requireUser(token);
        return userService.create(currentUser, request);
    }

    @PutMapping("/{id}")
    public UserResponse update(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long id,
            @Valid @RequestBody UserRequest request
    ) {
        AppUser currentUser = authService.requireUser(token);
        return userService.update(currentUser, id, request);
    }

    @PutMapping("/{id}/status")
    public UserResponse updateStatus(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> request
    ) {
        AppUser currentUser = authService.requireUser(token);
        if (!request.containsKey("enabled")) {
            throw new BusinessException("请选择账号状态");
        }
        return userService.updateStatus(currentUser, id, request.get("enabled"));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader("X-Auth-Token") String token, @PathVariable Long id) {
        AppUser currentUser = authService.requireUser(token);
        userService.delete(currentUser, id);
    }
}
