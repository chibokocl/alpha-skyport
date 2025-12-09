-- 10. payment_transactions
-- Detailed payment tracking with idempotency

CREATE TABLE payment_transactions (
    transaction_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shipment_id UUID NOT NULL REFERENCES shipments(shipment_id) ON DELETE CASCADE,
    
    -- Idempotency for payment gateways
    idempotency_key VARCHAR(100) UNIQUE NOT NULL,
    external_transaction_id VARCHAR(255),
    
    amount DECIMAL(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    
    transaction_type VARCHAR(20) NOT NULL CHECK (transaction_type IN (
        'payment', 'refund', 'chargeback', 'adjustment'
    )),
    
    payment_method VARCHAR(30) CHECK (payment_method IN (
        'credit_card', 'bank_transfer', 'mobile_money', 'cash', 'other'
    )),
    
    transaction_status VARCHAR(20) DEFAULT 'pending' CHECK (transaction_status IN (
        'pending', 'processing', 'completed', 'failed', 'cancelled', 'refunded'
    )),
    
    -- Gateway details
    gateway_provider VARCHAR(50),
    gateway_response JSONB,
    
    -- Processing
    processed_at TIMESTAMP,
    failed_reason TEXT,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payments_shipment ON payment_transactions(shipment_id);
CREATE INDEX idx_payments_idempotency ON payment_transactions(idempotency_key);
CREATE INDEX idx_payments_status ON payment_transactions(transaction_status);
CREATE INDEX idx_payments_external ON payment_transactions(external_transaction_id) 
    WHERE external_transaction_id IS NOT NULL;
