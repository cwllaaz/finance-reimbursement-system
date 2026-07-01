package com.geekworkshop.finance.service;

import com.geekworkshop.finance.config.PasswordMigrationInitializer;
import com.geekworkshop.finance.dto.LoginRequest;
import com.geekworkshop.finance.entity.AppUser;
import com.geekworkshop.finance.entity.UserRole;
import com.geekworkshop.finance.exception.BusinessException;
import com.geekworkshop.finance.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTests {
    @Mock AppUserRepository appUserRepository;
    @Mock OperationLogService operationLogService;

    private final PasswordService passwordService = new PasswordService();

    @Test
    void bcryptEncodesAndMatchesPassword() {
        String encoded = passwordService.encode("123456");

        assertTrue(passwordService.isEncoded(encoded));
        assertTrue(passwordService.matches("123456", encoded));
        assertFalse(passwordService.matches("wrong", encoded));
        assertNotEquals("123456", encoded);
    }

    @Test
    void startupMigrationEncodesLegacyPlaintextPasswords() throws Exception {
        AppUser legacy = user(1L, "123456");
        AppUser encoded = user(2L, passwordService.encode("abcdef"));
        when(appUserRepository.findAll()).thenReturn(List.of(legacy, encoded));
        PasswordMigrationInitializer initializer =
                new PasswordMigrationInitializer(appUserRepository, passwordService);

        initializer.run();

        assertTrue(passwordService.isEncoded(legacy.getPassword()));
        assertTrue(passwordService.matches("123456", legacy.getPassword()));
        assertTrue(passwordService.matches("abcdef", encoded.getPassword()));
    }

    @Test
    void logoutAndPasswordChangeInvalidationImmediatelyRevokeTokens() {
        AppUser user = user(1L, passwordService.encode("123456"));
        when(appUserRepository.findByUsername("employee")).thenReturn(Optional.of(user));
        when(appUserRepository.findWithDepartmentById(1L)).thenReturn(Optional.of(user));
        AuthService service = authService(120);

        String first = service.login(login()).getToken();
        String second = service.login(login()).getToken();
        assertEquals(1L, service.requireUser(first).getId());

        service.logout(first);
        assertThrows(BusinessException.class, () -> service.requireUser(first));
        assertEquals(1L, service.requireUser(second).getId());

        service.invalidateUserSessions(1L);
        assertThrows(BusinessException.class, () -> service.requireUser(second));
    }

    @Test
    void expiredTokenCannotBeUsed() {
        AppUser user = user(1L, passwordService.encode("123456"));
        when(appUserRepository.findByUsername("employee")).thenReturn(Optional.of(user));
        AuthService service = authService(0);

        String token = service.login(login()).getToken();

        assertThrows(BusinessException.class, () -> service.requireUser(token));
        verify(appUserRepository, never()).findWithDepartmentById(any());
    }

    @Test
    void secureFileValidationAllowsBusinessDocumentsAndRejectsDangerousFiles() {
        MockMultipartFile image = new MockMultipartFile(
                "file", "invoice.png", "image/png", new byte[]{1, 2, 3});
        assertEquals("invoice.png", SecureFileSupport.validate(image));

        MockMultipartFile executable = new MockMultipartFile(
                "file", "invoice.exe", "application/octet-stream", new byte[]{1});
        assertThrows(BusinessException.class, () -> SecureFileSupport.validate(executable));

        MockMultipartFile traversal = new MockMultipartFile(
                "file", "../invoice.pdf", "application/pdf", new byte[]{1});
        assertThrows(BusinessException.class, () -> SecureFileSupport.validate(traversal));

        MockMultipartFile mismatchedMime = new MockMultipartFile(
                "file", "invoice.pdf", "image/png", new byte[]{1});
        assertThrows(BusinessException.class, () -> SecureFileSupport.validate(mismatchedMime));

        MockMultipartFile oversized = new MockMultipartFile(
                "file", "invoice.pdf", "application/pdf",
                new byte[(int) SecureFileSupport.MAX_FILE_SIZE + 1]);
        assertThrows(BusinessException.class, () -> SecureFileSupport.validate(oversized));
    }

    private AuthService authService(long ttlMinutes) {
        AuthService service = new AuthService(appUserRepository, operationLogService, passwordService);
        ReflectionTestUtils.setField(service, "tokenTtlMinutes", ttlMinutes);
        return service;
    }

    private LoginRequest login() {
        LoginRequest request = new LoginRequest();
        request.setUsername("employee");
        request.setPassword("123456");
        return request;
    }

    private AppUser user(Long id, String password) {
        AppUser user = new AppUser();
        ReflectionTestUtils.setField(user, "id", id);
        user.setUsername("employee");
        user.setPassword(password);
        user.setRealName("测试用户");
        user.setRole(UserRole.EMPLOYEE);
        user.setEnabled(true);
        return user;
    }
}
