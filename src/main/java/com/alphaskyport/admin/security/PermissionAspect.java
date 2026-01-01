package com.alphaskyport.admin.security;

import com.alphaskyport.admin.exception.AdminException;
import com.alphaskyport.admin.model.AdminUser;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Collectors;

@Aspect
@Component
@Slf4j
public class PermissionAspect {

    @Before("@annotation(com.alphaskyport.admin.security.RequiresPermission)")
    public void checkPermission(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequiresPermission annotation = method.getAnnotation(RequiresPermission.class);
        
        String requiredPermission = annotation.value();
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AdminException.InsufficientPermissionsException("Not authenticated");
        }

        // Get current admin
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof AdminUser)) {
            throw new AdminException.InsufficientPermissionsException("Invalid authentication");
        }

        AdminUser admin = (AdminUser) principal;

        // Super admins have all permissions
        if (admin.getRole().hasPermission("*")) {
            return;
        }

        // Check if user has the required permission
        Set<String> userPermissions = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        if (!hasPermission(userPermissions, requiredPermission)) {
            log.warn("Permission denied for admin {} - required: {}, has: {}", 
                    admin.getEmail(), requiredPermission, userPermissions);
            throw new AdminException.InsufficientPermissionsException(
                    "Permission denied: " + requiredPermission);
        }
    }

    private boolean hasPermission(Set<String> userPermissions, String required) {
        // Exact match
        if (userPermissions.contains(required)) {
            return true;
        }

        // Wildcard match (e.g., "quotes:*" matches "quotes:read")
        String[] parts = required.split(":");
        if (parts.length == 2) {
            String wildcardPermission = parts[0] + ":*";
            if (userPermissions.contains(wildcardPermission)) {
                return true;
            }
        }

        // Global wildcard
        return userPermissions.contains("*");
    }
}
