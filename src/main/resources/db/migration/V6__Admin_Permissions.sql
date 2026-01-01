CREATE TABLE admin_users (
    admin_id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    requires_2fa BOOLEAN DEFAULT FALSE,
    totp_secret VARCHAR(255),
    failed_login_attempts INTEGER DEFAULT 0,
    locked_until TIMESTAMP,
    last_login TIMESTAMP,
    password_changed_at TIMESTAMP,
    version INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES admin_users(admin_id)
);

CREATE INDEX idx_admin_users_email ON admin_users(email);
CREATE INDEX idx_admin_users_role ON admin_users(role);

CREATE TABLE admin_permissions (
    permission_id SERIAL PRIMARY KEY,
    admin_id UUID NOT NULL REFERENCES admin_users(admin_id),
    permission_key VARCHAR(255) NOT NULL,
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    granted_by UUID REFERENCES admin_users(admin_id)
);

CREATE INDEX idx_admin_permissions_admin_id ON admin_permissions(admin_id);
