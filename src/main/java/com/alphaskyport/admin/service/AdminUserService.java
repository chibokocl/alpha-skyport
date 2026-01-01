package com.alphaskyport.admin.service;

import com.alphaskyport.admin.dto.AdminDTOs.*;
import com.alphaskyport.admin.model.*;
import com.alphaskyport.admin.repository.AdminUserRepository;
import com.alphaskyport.admin.exception.AdminException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class AdminUserService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminActivityService activityService;

    @Transactional(readOnly = true)
    public List<AdminUserResponse> getAllAdmins() {
        return adminUserRepository.findAllActiveOrderedByName()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getAdminById(UUID adminId) {
        AdminUser admin = adminUserRepository.findByIdWithPermissions(adminId)
                .orElseThrow(() -> new AdminException.AdminNotFoundException("Admin not found with ID: " + adminId));
        return mapToResponse(admin);
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> getAdminsByRole(AdminRole role) {
        return adminUserRepository.findByRoleAndIsActiveTrue(role)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AdminUserResponse createAdmin(CreateAdminRequest request, AdminUser createdBy) {
        // Check if email already exists
        if (adminUserRepository.existsByEmail(request.getEmail())) {
            throw new AdminException.DuplicateEmailException("Email already registered: " + request.getEmail());
        }

        // Validate role assignment (only super_admin can create other super_admins)
        if (request.getRole() == AdminRole.SUPER_ADMIN && createdBy.getRole() != AdminRole.SUPER_ADMIN) {
            throw new AdminException.InsufficientPermissionsException(
                    "Only super admins can create other super admins");
        }

        AdminUser admin = AdminUser.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .requires2fa(request.isRequires2fa())
                .createdBy(createdBy)
                .build();

        admin = adminUserRepository.save(admin);

        // Add additional permissions if specified
        if (request.getAdditionalPermissions() != null && !request.getAdditionalPermissions().isEmpty()) {
            AdminUser finalAdmin = admin;
            request.getAdditionalPermissions().forEach(permKey -> {
                AdminPermission permission = AdminPermission.builder()
                        .adminUser(finalAdmin)
                        .permissionKey(permKey)
                        .grantedBy(createdBy)
                        .build();
                finalAdmin.getPermissions().add(permission);
            });
            admin = adminUserRepository.save(admin);
        }

        activityService.logActivity(createdBy, "CREATE_ADMIN", "AdminUser", admin.getAdminId().toString(),
                "Created admin user: " + admin.getEmail(), null, null);

        log.info("Admin user created: {} by {}", admin.getEmail(), createdBy.getEmail());
        return mapToResponse(admin);
    }

    @Transactional
    public AdminUserResponse updateAdmin(UUID adminId, UpdateAdminRequest request, AdminUser updatedBy) {
        AdminUser admin = adminUserRepository.findById(adminId)
                .orElseThrow(() -> new AdminException.AdminNotFoundException("Admin not found with ID: " + adminId));

        // Prevent role escalation
        if (request.getRole() != null && request.getRole() == AdminRole.SUPER_ADMIN
                && updatedBy.getRole() != AdminRole.SUPER_ADMIN) {
            throw new AdminException.InsufficientPermissionsException("Only super admins can grant super admin role");
        }

        // Prevent self-deactivation for last super admin
        if (request.getIsActive() != null && !request.getIsActive()
                && admin.getRole() == AdminRole.SUPER_ADMIN) {
            long superAdminCount = adminUserRepository.countActiveByRole(AdminRole.SUPER_ADMIN);
            if (superAdminCount <= 1) {
                throw new AdminException.OperationNotAllowedException("Cannot deactivate the last super admin");
            }
        }

        if (request.getFirstName() != null)
            admin.setFirstName(request.getFirstName());
        if (request.getLastName() != null)
            admin.setLastName(request.getLastName());
        if (request.getPhone() != null)
            admin.setPhone(request.getPhone());
        if (request.getRole() != null)
            admin.setRole(request.getRole());
        if (request.getIsActive() != null)
            admin.setIsActive(request.getIsActive());
        if (request.getRequires2fa() != null)
            admin.setRequires2fa(request.getRequires2fa());

        admin = adminUserRepository.save(admin);

        activityService.logActivity(updatedBy, "UPDATE_ADMIN", "AdminUser", admin.getAdminId().toString(),
                "Updated admin user: " + admin.getEmail(), null, null);

        return mapToResponse(admin);
    }

    @Transactional
    public void updatePermissions(UUID adminId, PermissionUpdateRequest request, AdminUser updatedBy) {
        AdminUser admin = adminUserRepository.findByIdWithPermissions(adminId)
                .orElseThrow(() -> new AdminException.AdminNotFoundException("Admin not found with ID: " + adminId));

        // Clear existing additional permissions
        admin.getPermissions().clear();

        // Add new permissions
        if (request.getPermissions() != null) {
            request.getPermissions().forEach(permKey -> {
                AdminPermission permission = AdminPermission.builder()
                        .adminUser(admin)
                        .permissionKey(permKey)
                        .grantedBy(updatedBy)
                        .build();
                admin.getPermissions().add(permission);
            });
        }

        adminUserRepository.save(admin);

        activityService.logActivity(updatedBy, "UPDATE_PERMISSIONS", "AdminUser", admin.getAdminId().toString(),
                "Updated permissions for: " + admin.getEmail(), null, null);
    }

    @Transactional
    public void deactivateAdmin(UUID adminId, AdminUser deactivatedBy) {
        AdminUser admin = adminUserRepository.findById(adminId)
                .orElseThrow(() -> new AdminException.AdminNotFoundException("Admin not found with ID: " + adminId));

        // Prevent self-deactivation
        if (admin.getAdminId().equals(deactivatedBy.getAdminId())) {
            throw new AdminException.OperationNotAllowedException("Cannot deactivate your own account");
        }

        // Prevent deactivation of last super admin
        if (admin.getRole() == AdminRole.SUPER_ADMIN) {
            long superAdminCount = adminUserRepository.countActiveByRole(AdminRole.SUPER_ADMIN);
            if (superAdminCount <= 1) {
                throw new AdminException.OperationNotAllowedException("Cannot deactivate the last super admin");
            }
        }

        admin.setIsActive(false);
        adminUserRepository.save(admin);

        activityService.logActivity(deactivatedBy, "DEACTIVATE_ADMIN", "AdminUser", admin.getAdminId().toString(),
                "Deactivated admin user: " + admin.getEmail(), null, null);

        log.info("Admin user deactivated: {} by {}", admin.getEmail(), deactivatedBy.getEmail());
    }

    @Transactional
    public void resetPassword(UUID adminId, String newPassword, AdminUser resetBy) {
        AdminUser admin = adminUserRepository.findById(adminId)
                .orElseThrow(() -> new AdminException.AdminNotFoundException("Admin not found with ID: " + adminId));

        admin.setPasswordHash(passwordEncoder.encode(newPassword));
        admin.resetFailedAttempts();
        adminUserRepository.save(admin);

        activityService.logActivity(resetBy, "RESET_PASSWORD", "AdminUser", admin.getAdminId().toString(),
                "Reset password for: " + admin.getEmail(), null, null);
    }

    @Transactional
    public void unlockAccount(UUID adminId, AdminUser unlockedBy) {
        AdminUser admin = adminUserRepository.findById(adminId)
                .orElseThrow(() -> new AdminException.AdminNotFoundException("Admin not found with ID: " + adminId));

        admin.resetFailedAttempts();
        adminUserRepository.save(admin);

        activityService.logActivity(unlockedBy, "UNLOCK_ACCOUNT", "AdminUser", admin.getAdminId().toString(),
                "Unlocked account for: " + admin.getEmail(), null, null);
    }

    private AdminUserResponse mapToResponse(AdminUser admin) {
        Set<String> permissions = admin.getPermissions().stream()
                .map(AdminPermission::getPermissionKey)
                .collect(Collectors.toSet());

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
