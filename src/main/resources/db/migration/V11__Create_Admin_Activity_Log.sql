CREATE TABLE admin_activity_log (
    log_id BIGSERIAL PRIMARY KEY,
    admin_id UUID REFERENCES admin_users(admin_id),
    activity_type VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50),
    entity_id VARCHAR(255),
    description TEXT,
    metadata JSONB,
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_admin_activity_log_admin_id ON admin_activity_log(admin_id);
CREATE INDEX idx_admin_activity_log_activity_type ON admin_activity_log(activity_type);
CREATE INDEX idx_admin_activity_log_created_at ON admin_activity_log(created_at);
