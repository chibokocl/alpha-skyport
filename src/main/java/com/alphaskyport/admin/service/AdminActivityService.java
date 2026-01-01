package com.alphaskyport.admin.service;

import com.alphaskyport.admin.model.AdminUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminActivityService {

    private final JdbcTemplate jdbcTemplate;

    @Async
    public void logActivity(AdminUser admin, String activityType, String entityType, 
                           String entityId, String description, String ipAddress, String userAgent) {
        try {
            String sql = """
                INSERT INTO admin_activity_log 
                (admin_id, activity_type, entity_type, entity_id, description, metadata, ip_address, user_agent)
                VALUES (?, ?, ?, ?, ?, ?::jsonb, ?::inet, ?)
                """;

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("timestamp", System.currentTimeMillis());

            jdbcTemplate.update(sql,
                    admin != null ? admin.getAdminId() : null,
                    activityType,
                    entityType,
                    entityId,
                    description,
                    "{}",
                    ipAddress,
                    userAgent
            );
        } catch (Exception e) {
            log.error("Failed to log admin activity: {}", e.getMessage());
        }
    }

    public void logLogin(AdminUser admin, String ipAddress, String userAgent, boolean success) {
        logActivity(admin, success ? "LOGIN_SUCCESS" : "LOGIN_FAILED", "AdminUser",
                admin.getAdminId().toString(), 
                success ? "Successful login" : "Failed login attempt",
                ipAddress, userAgent);
    }

    public void logLogout(AdminUser admin) {
        logActivity(admin, "LOGOUT", "AdminUser", admin.getAdminId().toString(),
                "User logged out", null, null);
    }

    public void logDataAccess(AdminUser admin, String entityType, String entityId, String action) {
        logActivity(admin, "DATA_ACCESS", entityType, entityId,
                action + " " + entityType + ": " + entityId, null, null);
    }

    public void logConfigChange(AdminUser admin, String settingKey, String oldValue, String newValue) {
        logActivity(admin, "CONFIG_CHANGE", "SystemSetting", settingKey,
                "Changed " + settingKey + " from '" + oldValue + "' to '" + newValue + "'", null, null);
    }
}
