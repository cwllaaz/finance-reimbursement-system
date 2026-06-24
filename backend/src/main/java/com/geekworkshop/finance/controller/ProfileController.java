package com.geekworkshop.finance.controller;

import com.geekworkshop.finance.dto.PasswordChangeRequest;
import com.geekworkshop.finance.dto.ProfileRequest;
import com.geekworkshop.finance.dto.UserResponse;
import com.geekworkshop.finance.entity.AppUser;
import com.geekworkshop.finance.service.AuthService;
import com.geekworkshop.finance.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;
    private final AuthService authService;

    public ProfileController(ProfileService profileService, AuthService authService) {
        this.profileService = profileService;
        this.authService = authService;
    }

    @GetMapping
    public UserResponse getProfile(@RequestHeader("X-Auth-Token") String token) {
        AppUser currentUser = authService.requireUser(token);
        return profileService.getProfile(currentUser);
    }

    @PutMapping
    public UserResponse updateProfile(
            @RequestHeader("X-Auth-Token") String token,
            @Valid @RequestBody ProfileRequest request
    ) {
        AppUser currentUser = authService.requireUser(token);
        return profileService.updateProfile(currentUser, request);
    }

    @PutMapping("/password")
    public Map<String, Object> changePassword(
            @RequestHeader("X-Auth-Token") String token,
            @Valid @RequestBody PasswordChangeRequest request
    ) {
        AppUser currentUser = authService.requireUser(token);
        profileService.changePassword(currentUser, request);
        return Map.of("success", true);
    }
}
