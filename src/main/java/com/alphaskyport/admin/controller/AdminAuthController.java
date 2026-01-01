package com.alphaskyport.admin.controller;

import com.alphaskyport.admin.dto.AdminDTOs.*;
import com.alphaskyport.admin.model.AdminUser;
import com.alphaskyport.admin.service.AdminAuthService;
import com.alphaskyport.admin.security.CurrentAdmin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
@Tag(name = "Admin Authentication", description = "Admin authentication endpoints")
public class AdminAuthController {

    private final AdminAuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Admin login", description = "Authenticate admin user and get tokens")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        
        LoginResponse response = authService.login(request, ipAddress, userAgent);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Get new access token using refresh token")
    public ResponseEntity<LoginResponse> refreshToken(@RequestParam String refreshToken) {
        LoginResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Invalidate current session")
    public ResponseEntity<Void> logout(
            @CurrentAdmin AdminUser admin,
            @RequestHeader("Authorization") String authHeader) {
        
        String token = authHeader.replace("Bearer ", "");
        authService.logout(admin.getAdminId(), token);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password", description = "Change current admin's password")
    public ResponseEntity<Void> changePassword(
            @CurrentAdmin AdminUser admin,
            @Valid @RequestBody ChangePasswordRequest request) {
        
        authService.changePassword(admin.getAdminId(), request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Get current admin", description = "Get currently authenticated admin details")
    public ResponseEntity<AdminUserResponse> getCurrentAdmin(@CurrentAdmin AdminUser admin) {
        // The admin is already fetched by the security context
        return ResponseEntity.ok(AdminUserResponse.builder()
                .adminId(admin.getAdminId())
                .email(admin.getEmail())
                .role(admin.getRole())
                .firstName(admin.getFirstName())
                .lastName(admin.getLastName())
                .fullName(admin.getFullName())
                .isActive(admin.getIsActive())
                .requires2fa(admin.getRequires2fa())
                .lastLogin(admin.getLastLogin())
                .build());
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
