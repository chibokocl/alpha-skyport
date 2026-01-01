CREATE TABLE pricing_rules (
    rule_id UUID PRIMARY KEY,
    rule_name VARCHAR(255) NOT NULL,
    rule_type VARCHAR(50) NOT NULL,
    description TEXT,
    conditions JSONB NOT NULL,
    adjustment_type VARCHAR(50) NOT NULL,
    adjustment_value DECIMAL(12, 2) NOT NULL,
    priority INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    valid_from TIMESTAMP,
    valid_until TIMESTAMP,
    created_by UUID REFERENCES admin_users(admin_id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pricing_rules_active ON pricing_rules(is_active);
CREATE INDEX idx_pricing_rules_priority ON pricing_rules(priority);
CREATE INDEX idx_pricing_rules_validity ON pricing_rules(valid_from, valid_until);
