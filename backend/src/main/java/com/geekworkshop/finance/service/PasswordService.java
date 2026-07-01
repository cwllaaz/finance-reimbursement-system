package com.geekworkshop.finance.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String storedPassword) {
        return storedPassword != null && (isEncoded(storedPassword)
                ? encoder.matches(rawPassword, storedPassword)
                : storedPassword.equals(rawPassword));
    }

    public boolean isEncoded(String password) {
        return password != null && password.matches("^\\$2[aby]\\$\\d{2}\\$.*");
    }
}
