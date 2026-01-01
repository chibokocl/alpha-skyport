-- Invoices Table
CREATE TABLE invoices (
    invoice_id UUID PRIMARY KEY,
    invoice_number VARCHAR(50) UNIQUE NOT NULL,
    user_id UUID NOT NULL REFERENCES users(user_id),
    shipment_id UUID REFERENCES shipments(shipment_id),
    status VARCHAR(20) NOT NULL,
    subtotal DECIMAL(12, 2) NOT NULL,
    tax_amount DECIMAL(12, 2) DEFAULT 0,
    total_amount DECIMAL(12, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    paid_date DATE,
    paid_amount DECIMAL(12, 2) DEFAULT 0,
    notes TEXT,
    created_by UUID REFERENCES admin_users(admin_id),
    version INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_invoices_user ON invoices(user_id);
CREATE INDEX idx_invoices_shipment ON invoices(shipment_id);
CREATE INDEX idx_invoices_status ON invoices(status);
CREATE INDEX idx_invoices_due_date ON invoices(due_date);

-- Invoice Line Items Table
CREATE TABLE invoice_line_items (
    line_item_id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL REFERENCES invoices(invoice_id) ON DELETE CASCADE,
    description VARCHAR(255) NOT NULL,
    quantity DECIMAL(10, 2) DEFAULT 1,
    unit_price DECIMAL(12, 2) NOT NULL,
    tax_rate DECIMAL(5, 2) DEFAULT 0,
    line_total DECIMAL(12, 2) NOT NULL,
    sort_order INTEGER DEFAULT 0
);

CREATE INDEX idx_invoice_lines_invoice ON invoice_line_items(invoice_id);

-- Payments Table (Admin module payments)
CREATE TABLE payments (
    payment_id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL REFERENCES invoices(invoice_id),
    amount DECIMAL(12, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    payment_method VARCHAR(50) NOT NULL,
    payment_reference VARCHAR(100),
    payment_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    notes TEXT,
    recorded_by UUID REFERENCES admin_users(admin_id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_admin_payments_invoice ON payments(invoice_id);
