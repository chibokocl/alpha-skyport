-- 11. notification_queue (Async Processing)
-- Deduplication and retry logic

CREATE TABLE notification_queue (
    queue_id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    shipment_id UUID REFERENCES shipments(shipment_id) ON DELETE CASCADE,
    
    notification_type VARCHAR(50) NOT NULL,
    
    -- Deduplication
    dedup_key VARCHAR(200) UNIQUE NOT NULL,
    
    -- Content
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    metadata JSONB,
    
    -- Delivery channels
    send_via VARCHAR(20)[] DEFAULT ARRAY['in_app'], -- Array of channels
    
    -- Status
    status VARCHAR(20) DEFAULT 'pending' CHECK (status IN (
        'pending', 'processing', 'sent', 'failed', 'cancelled'
    )),
    
    -- Retry logic
    attempts INTEGER DEFAULT 0,
    max_attempts INTEGER DEFAULT 3,
    next_retry_at TIMESTAMP,
    last_error TEXT,
    
    -- Delivery tracking
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    read_at TIMESTAMP,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notif_queue_status ON notification_queue(status, next_retry_at) 
    WHERE status IN ('pending', 'failed');
CREATE INDEX idx_notif_queue_user ON notification_queue(user_id);
CREATE INDEX idx_notif_queue_dedup ON notification_queue(dedup_key);
