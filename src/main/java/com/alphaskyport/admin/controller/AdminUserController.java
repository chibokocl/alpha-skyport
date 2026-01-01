package com.alphaskyport.admin.controller;

import com.alphaskyport.admin.dto.AdminDTOs.*;
import com.alphaskyport.admin.model.AdminRole;
import com.alphaskyport.admin.model.AdminUser;
import com.alphaskyport.admin.service.AdminUserService;
import com.alphaskyport.admin.security.CurrentAdmin;
import com.alphaskyport.admin.security.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin User Management", description = "Manage admin users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @RequiresPermission("admin:read")
    @Operation(summary = "List all admins", description = "Get list of all admin users")
    public ResponseEntity<List<AdminUserResponse>> getAllAdmins() {
        return ResponseEntity.ok(adminUserService.getAllAdmins());
    }

    @GetMapping("/{adminId}")
    @RequiresPermission("admin:read")
    @Operation(summary = "Get admin by ID", description = "Get admin user details")
    public ResponseEntity<AdminUserResponse> getAdminById(@PathVariable UUID adminId) {
        return ResponseEntity.ok(adminUserService.getAdminById(adminId));
    }

    @GetMapping("/role/{role}")
    @RequiresPermission("admin:read")
    @Operation(summary = "Get admins by role", description = "Get admin users by role")
    public ResponseEntity<List<AdminUserResponse>> getAdminsByRole(@PathVariable AdminRole role) {
        return ResponseEntity.ok(adminUserService.getAdminsByRole(role));
    }

    @PostMapping
    @RequiresPermission("admin:write")
    @Operation(summary = "Create admin", description = "Create a new admin user")
    public ResponseEntity<AdminUserResponse> createAdmin(
            @Valid @RequestBody CreateAdminRequest request,
            @CurrentAdmin AdminUser currentAdmin) {
        return ResponseEntity.ok(adminUserService.createAdmin(request, currentAdmin));
    }

    @PutMapping("/{adminId}")
    @RequiresPermission("admin:write")
    @Operation(summary = "Update admin", description = "Update admin user details")
    public ResponseEntity<AdminUserResponse> updateAdmin(
            @PathVariable UUID adminId,
            @Valid @RequestBody UpdateAdminRequest request,
            @CurrentAdmin AdminUser currentAdmin) {
        return ResponseEntity.ok(adminUserService.updateAdmin(adminId, request, currentAdmin));
    }

    @PutMapping("/{adminId}/permissions")
    @RequiresPermission("admin:write")
    @Operation(summary = "Update permissions", description = "Update admin user permissions")
    public ResponseEntity<Void> updatePermissions(
            @PathVariable UUID adminId,
            @Valid @RequestBody PermissionUpdateRequest request,
            @CurrentAdmin AdminUser currentAdmin) {
        adminUserService.updatePermissions(adminId, request, currentAdmin);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{adminId}/deactivate")
    @RequiresPermission("admin:write")
    @Operation(summary = "Deactivate admin", description = "Deactivate an admin user")
    public ResponseEntity<Void> deactivateAdmin(
            @PathVariable UUID adminId,
            @CurrentAdmin AdminUser currentAdmin) {
        adminUserService.deactivateAdmin(adminId, currentAdmin);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{adminId}/reset-password")
    @RequiresPermission("admin:write")
    @Operation(summary = "Reset password", description = "Reset admin user password")
    public ResponseEntity<Void> resetPassword(
            @PathVariable UUID adminId,
            @RequestParam String newPassword,
            @CurrentAdmin AdminUser currentAdmin) {
        adminUserService.resetPassword(adminId, newPassword, currentAdmin);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{adminId}/unlock")
    @RequiresPermission("admin:write")
    @Operation(summary = "Unlock account", description = "Unlock a locked admin account")
    public ResponseEntity<Void> unlockAccount(
            @PathVariable UUID adminId,
            @CurrentAdmin AdminUser currentAdmin) {
        adminUserService.unlockAccount(adminId, currentAdmin);
        return ResponseEntity.ok().build();
    }
}
