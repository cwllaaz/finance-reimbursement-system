package com.geekworkshop.finance.config;

import com.geekworkshop.finance.repository.AppUserRepository;
import com.geekworkshop.finance.service.PasswordService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(100)
public class PasswordMigrationInitializer implements CommandLineRunner {
    private final AppUserRepository appUserRepository;
    private final PasswordService passwordService;

    public PasswordMigrationInitializer(AppUserRepository appUserRepository, PasswordService passwordService) {
        this.appUserRepository = appUserRepository;
        this.passwordService = passwordService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        appUserRepository.findAll().stream()
                .filter(user -> !passwordService.isEncoded(user.getPassword()))
                .forEach(user -> user.setPassword(passwordService.encode(user.getPassword())));
    }
}
