package com.alphaskyport.admin.model;

import java.util.Set;

public enum AdminRole {
    SUPER_ADMIN("super_admin", Set.of("*")),
    ADMIN("admin", Set.of(
        "quotes:*", "shipments:*", "users:*", "pricing:*", 
        "invoices:*", "payments:*", "reports:*", "admin:read"
    )),
    OPERATIONS("operations", Set.of(
        "quotes:read", "quotes:write", "quotes:approve",
        "shipments:read", "shipments:write",
        "users:read"
    )),
    SUPPORT("support", Set.of(
        "quotes:read", "shipments:read", "users:read", "users:write"
    )),
    FINANCE("finance", Set.of(
        "quotes:read", "shipments:read", "users:read",
        "invoices:*", "payments:*", "reports:*"
    )),
    VIEWER("viewer", Set.of(
        "quotes:read", "shipments:read", "users:read", "reports:view"
    ));

    private final String value;
    private final Set<String> defaultPermissions;

    AdminRole(String value, Set<String> defaultPermissions) {
        this.value = value;
        this.defaultPermissions = defaultPermissions;
    }

    public String getValue() {
        return value;
    }

    public Set<String> getDefaultPermissions() {
        return defaultPermissions;
    }

    public boolean hasPermission(String permission) {
        if (defaultPermissions.contains("*")) return true;
        
        for (String p : defaultPermissions) {
            if (p.equals(permission)) return true;
            if (p.endsWith(":*")) {
                String prefix = p.substring(0, p.length() - 1);
                if (permission.startsWith(prefix)) return true;
            }
        }
        return false;
    }

    public static AdminRole fromValue(String value) {
        for (AdminRole role : values()) {
            if (role.value.equals(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + value);
    }
}
