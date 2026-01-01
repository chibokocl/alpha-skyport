CREATE TABLE shipment_issues (
    issue_id UUID PRIMARY KEY,
    shipment_id UUID NOT NULL REFERENCES shipments(shipment_id),
    issue_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    reported_by UUID REFERENCES admin_users(admin_id),
    assigned_to UUID REFERENCES admin_users(admin_id),
    status VARCHAR(20) DEFAULT 'OPEN',
    responsible_party VARCHAR(50),
    financial_impact DECIMAL(12, 2),
    resolution_notes TEXT,
    resolved_at TIMESTAMP,
    resolved_by UUID REFERENCES admin_users(admin_id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_shipment_issues_shipment ON shipment_issues(shipment_id);
CREATE INDEX idx_shipment_issues_status ON shipment_issues(status);
CREATE INDEX idx_shipment_issues_assigned ON shipment_issues(assigned_to);

CREATE TABLE issue_attachments (
    attachment_id UUID PRIMARY KEY,
    issue_id UUID NOT NULL REFERENCES shipment_issues(issue_id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(512) NOT NULL,
    file_type VARCHAR(100),
    file_size INTEGER,
    uploaded_by UUID REFERENCES admin_users(admin_id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_issue_attachments_issue ON issue_attachments(issue_id);
