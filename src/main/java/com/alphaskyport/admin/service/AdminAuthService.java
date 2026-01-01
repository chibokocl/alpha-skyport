package com.alphaskyport.admin.service;

import com.alphaskyport.admin.dto.AdminDTOs.*;
import com.alphaskyport.admin.model.*;
import com.alphaskyport.admin.repository.AdminUserRepository;
import com.alphaskyport.admin.security.JwtTokenProvider;
import com.alphaskyport.admin.exception.AdminException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class AdminAuthService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AdminActivityService activityService;

    @Value("${admin.security.max-login-attempts:5}")
    private int maxLoginAttempts;

    @Value("${admin.security.lockout-duration-minutes:30}")
    private int lockoutDurationMinutes;

    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        AdminUser admin = adminUserRepository.findByEmailWithPermissions(request.getEmail())
                .orElseThrow(() -> new AdminException.InvalidCredentialsException("Invalid email or password"));

        // Check if account is locked
        if (admin.isLocked()) {
            throw new AdminException.AccountLockedException("Account is locked. Try again later.");
        }

        // Check if account is active
        if (!admin.getIsActive()) {
            throw new AdminException.AccountDisabledException("Account is disabled.");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), admin.getPasswordHash())) {
            handleFailedLogin(admin);
            throw new AdminException.InvalidCredentialsException("Invalid email or password");
        }

        // Check 2FA if required
        if (admin.getRequires2fa() && (request.getTotpCode() == null || request.getTotpCode().isEmpty())) {
            return LoginResponse.builder()
                    .requires2fa(true)
                    .build();
        }

        // Note: TOTP validation logic to be implemented if 2FA is enabled
        if (admin.getRequires2fa()) {
            log.warn("TOTP validation skipped for user {} - logic not implemented", request.getEmail());
        }

        // Reset failed attempts on successful login
        admin.resetFailedAttempts();
        admin.setLastLogin(LocalDateTime.now());
        adminUserRepository.save(admin);

        // Generate tokens
        String accessToken = jwtTokenProvider.generateAccessToken(admin);
        String refreshToken = jwtTokenProvider.generateRefreshToken(admin);

        // Log activity
        activityService.logActivity(admin, "LOGIN", "AdminUser", admin.getAdminId().toString(),
                "User logged in", ipAddress, userAgent);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
                .user(mapToResponse(admin))
                .requires2fa(false)
                .build();
    }

    @Transactional
    public LoginResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new AdminException.InvalidTokenException("Invalid or expired refresh token");
        }

        UUID adminId = jwtTokenProvider.getAdminIdFromToken(refreshToken);
        AdminUser admin = adminUserRepository.findByIdWithPermissions(adminId)
                .orElseThrow(() -> new AdminException.AdminNotFoundException("Admin not found"));

        if (!admin.getIsActive()) {
            throw new AdminException.AccountDisabledException("Account is disabled");
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(admin);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(admin);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
                .user(mapToResponse(admin))
                .build();
    }

    @Transactional
    public void logout(UUID adminId, String token) {
        jwtTokenProvider.revokeToken(token);
        AdminUser admin = adminUserRepository.findById(adminId).orElse(null);
        if (admin != null) {
            activityService.logActivity(admin, "LOGOUT", "AdminUser", adminId.toString(),
                    "User logged out", null, null);
        }
    }

    @Transactional
    public void changePassword(UUID adminId, ChangePasswordRequest request) {
        AdminUser admin = adminUserRepository.findById(adminId)
                .orElseThrow(() -> new AdminException.AdminNotFoundException("Admin not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), admin.getPasswordHash())) {
            throw new AdminException.InvalidCredentialsException("Current password is incorrect");
        }

        admin.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        admin.setPasswordChangedAt(LocalDateTime.now());
        adminUserRepository.save(admin);

        activityService.logActivity(admin, "PASSWORD_CHANGE", "AdminUser", adminId.toString(),
                "Password changed", null, null);
    }

    private void handleFailedLogin(AdminUser admin) {
        admin.incrementFailedAttempts();

        if (admin.getFailedLoginAttempts() >= maxLoginAttempts) {
            admin.lock(lockoutDurationMinutes);
            log.warn("Account locked due to too many failed attempts: {}", admin.getEmail());
        }

        adminUserRepository.save(admin);
    }

    private AdminUserResponse mapToResponse(AdminUser admin) {
        Set<String> permissions = admin.getPermissions().stream()
                .map(AdminPermission::getPermissionKey)
                .collect(Collectors.toSet());

        // Add role-based default permissions
        permissions.addAll(admin.getRole().getDefaultPermissions());

        return AdminUserResponse.builder()
                .adminId(admin.getAdminId())
                .email(admin.getEmail())
                .role(admin.getRole())
                .firstName(admin.getFirstName())
                .lastName(admin.getLastName())
                .fullName(admin.getFullName())
                .phone(admin.getPhone())
                .isActive(admin.getIsActive())
                .requires2fa(admin.getRequires2fa())
                .lastLogin(admin.getLastLogin())
                .createdAt(admin.getCreatedAt())
                .permissions(permissions)
                .build();
    }
}
