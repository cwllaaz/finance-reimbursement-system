package com.geekworkshop.finance.controller;

import com.geekworkshop.finance.dto.LoginRequest;
import com.geekworkshop.finance.dto.LoginResponse;
import com.geekworkshop.finance.dto.UserInfoResponse;
import com.geekworkshop.finance.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserInfoResponse me(@RequestHeader("X-Auth-Token") String token) {
        return UserInfoResponse.fromEntity(authService.requireUser(token));
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        authService.logout(token);
        return Map.of("success", true);
    }
}
