-- 4. quotes (With Optimistic Locking)
CREATE TABLE quotes (
    quote_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    
    -- Idempotency
    idempotency_key VARCHAR(100) UNIQUE,
    
    origin_country_id INTEGER NOT NULL REFERENCES countries(country_id),
    destination_country_id INTEGER NOT NULL REFERENCES countries(country_id),
    service_id INTEGER REFERENCES freight_services(service_id),
    is_residential BOOLEAN DEFAULT FALSE,
    
    cargo_description TEXT,
    cargo_weight DECIMAL(10, 2),
    cargo_weight_unit VARCHAR(10) DEFAULT 'kg',
    cargo_volume DECIMAL(10, 2),
    cargo_volume_unit VARCHAR(10) DEFAULT 'm3',
    cargo_value DECIMAL(12, 2),
    cargo_currency VARCHAR(3) DEFAULT 'USD',
    special_requirements TEXT,
    
    quote_status VARCHAR(20) DEFAULT 'pending' CHECK (quote_status IN (
        'pending', 'calculating', 'quoted', 'accepted', 'rejected', 'expired', 'converted'
    )),
    
    -- Pricing snapshot (immutable once quoted)
    pricing_snapshot JSONB,
    quoted_price DECIMAL(12, 2),
    quoted_at TIMESTAMP,
    valid_until TIMESTAMP,
    
    -- Prevent double-conversion
    converted_to_shipment_id UUID,
    converted_at TIMESTAMP,
    
    -- Concurrency Control
    version INTEGER DEFAULT 1 NOT NULL,
    
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraint: Can't be converted twice
    CONSTRAINT quote_single_conversion CHECK (
        (converted_to_shipment_id IS NULL AND converted_at IS NULL) OR
        (converted_to_shipment_id IS NOT NULL AND converted_at IS NOT NULL)
    )
);

CREATE INDEX idx_quotes_user ON quotes(user_id);
CREATE INDEX idx_quotes_status ON quotes(quote_status);
CREATE INDEX idx_quotes_idempotency ON quotes(idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE INDEX idx_quotes_valid_until ON quotes(valid_until) WHERE quote_status = 'quoted';

-- Trigger for version increment (reusing function from V1)
CREATE TRIGGER quotes_version_trigger
BEFORE UPDATE ON quotes
FOR EACH ROW
EXECUTE FUNCTION increment_version();

-- 5. tracking_number_sequence
-- Atomic tracking number generation

CREATE SEQUENCE tracking_number_seq
    START WITH 1000000
    INCREMENT BY 1
    NO CYCLE;

CREATE TABLE tracking_numbers (
    tracking_id BIGSERIAL PRIMARY KEY,
    tracking_number VARCHAR(50) UNIQUE NOT NULL,
    shipment_id UUID UNIQUE,
    allocated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    used_at TIMESTAMP,
    
    -- Prevent reuse
    is_used BOOLEAN DEFAULT FALSE,
    
    CONSTRAINT tracking_must_be_used CHECK (
        (is_used = FALSE AND used_at IS NULL) OR
        (is_used = TRUE AND used_at IS NOT NULL)
    )
);

CREATE INDEX idx_tracking_unused ON tracking_numbers(is_used) WHERE is_used = FALSE;

-- Function to generate tracking number atomically
CREATE OR REPLACE FUNCTION generate_tracking_number()
RETURNS VARCHAR(50) AS $$
DECLARE
    new_number VARCHAR(50);
    seq_val BIGINT;
BEGIN
    -- Get next sequence value
    seq_val := nextval('tracking_number_seq');
    
    -- Format: ASL-YYYY-XXXXXXX (Alpha Skyport Ltd - Year - Number)
    new_number := 'ASL-' || 
                  TO_CHAR(CURRENT_DATE, 'YYYY') || '-' || 
                  LPAD(seq_val::TEXT, 7, '0');
    
    -- Reserve the number
    INSERT INTO tracking_numbers (tracking_number, is_used)
    VALUES (new_number, FALSE);
    
    RETURN new_number;
END;
$$ LANGUAGE plpgsql;

-- 6. shipments (Enhanced with Locking)
CREATE TABLE shipments (
    shipment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tracking_number VARCHAR(50) UNIQUE NOT NULL,
    
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    quote_id UUID REFERENCES quotes(quote_id),
    service_id INTEGER NOT NULL REFERENCES freight_services(service_id),
    
    origin_country_id INTEGER NOT NULL REFERENCES countries(country_id),
    destination_country_id INTEGER NOT NULL REFERENCES countries(country_id),
    origin_address TEXT,
    destination_address TEXT,
    is_residential_delivery BOOLEAN DEFAULT FALSE,
    
    cargo_description TEXT,
    cargo_weight DECIMAL(10, 2),
    cargo_weight_unit VARCHAR(10) DEFAULT 'kg',
    cargo_volume DECIMAL(10, 2),
    cargo_volume_unit VARCHAR(10) DEFAULT 'm3',
    declared_value DECIMAL(12, 2),
    currency VARCHAR(3) DEFAULT 'USD',
    
    shipment_status VARCHAR(30) DEFAULT 'pending' CHECK (shipment_status IN (
        'pending', 'confirmed', 'picked_up', 'in_transit', 
        'customs_clearance', 'out_for_delivery', 'delivered', 
        'cancelled', 'returned', 'exception'
    )),
    
    -- Status machine - prevent invalid transitions
    previous_status VARCHAR(30),
    status_changed_at TIMESTAMP,
    
    estimated_pickup_date DATE,
    actual_pickup_date DATE,
    estimated_delivery_date DATE,
    actual_delivery_date DATE,
    
    total_cost DECIMAL(12, 2),
    payment_status VARCHAR(20) DEFAULT 'unpaid' CHECK (payment_status IN (
        'unpaid', 'pending', 'partial', 'paid', 'refunded', 'failed'
    )),
    
    -- Payment tracking
    amount_paid DECIMAL(12, 2) DEFAULT 0,
    amount_due DECIMAL(12, 2),
    
    special_instructions TEXT,
    
    -- Concurrency Control
    version INTEGER DEFAULT 1 NOT NULL,
    
    -- Soft delete
    deleted_at TIMESTAMP,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT shipment_payment_check CHECK (
        amount_paid <= total_cost AND amount_due >= 0
    )
);

CREATE INDEX idx_shipments_tracking ON shipments(tracking_number);
CREATE INDEX idx_shipments_user ON shipments(user_id);
CREATE INDEX idx_shipments_status ON shipments(shipment_status);
CREATE INDEX idx_shipments_dates ON shipments(estimated_delivery_date);
CREATE INDEX idx_shipments_payment ON shipments(payment_status) WHERE payment_status != 'paid';
CREATE INDEX idx_shipments_active ON shipments(deleted_at) WHERE deleted_at IS NULL;

CREATE TRIGGER shipments_version_trigger
BEFORE UPDATE ON shipments
FOR EACH ROW
EXECUTE FUNCTION increment_version();

-- Status transition tracking
CREATE OR REPLACE FUNCTION track_status_change()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.shipment_status != NEW.shipment_status THEN
        NEW.previous_status = OLD.shipment_status;
        NEW.status_changed_at = CURRENT_TIMESTAMP;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER shipment_status_trigger
BEFORE UPDATE ON shipments
FOR EACH ROW
EXECUTE FUNCTION track_status_change();

-- 7. shipment_tracking_events (Partitioned)
-- Event sourcing pattern - immutable events

CREATE TABLE shipment_tracking_events (
    event_id BIGSERIAL,
    shipment_id UUID NOT NULL REFERENCES shipments(shipment_id) ON DELETE CASCADE,
    
    -- Event details
    event_status VARCHAR(30) NOT NULL,
    event_location VARCHAR(255),
    event_country_id INTEGER REFERENCES countries(country_id),
    event_description TEXT,
    
    -- GPS
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    
    -- Event metadata
    event_timestamp TIMESTAMP NOT NULL,
    event_source VARCHAR(50), -- 'system', 'manual', 'api', 'gps'
    
    -- Idempotency for external systems
    external_event_id VARCHAR(100),
    
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (event_id, event_timestamp)
) PARTITION BY RANGE (event_timestamp);

-- Create partitions for each month (example for 2025)
CREATE TABLE shipment_tracking_events_2025_01 PARTITION OF shipment_tracking_events
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');

CREATE TABLE shipment_tracking_events_2025_02 PARTITION OF shipment_tracking_events
    FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');

CREATE TABLE shipment_tracking_events_2025_03 PARTITION OF shipment_tracking_events
    FOR VALUES FROM ('2025-03-01') TO ('2025-04-01');

CREATE TABLE shipment_tracking_events_2025_04 PARTITION OF shipment_tracking_events FOR VALUES FROM ('2025-04-01') TO ('2025-05-01');
CREATE TABLE shipment_tracking_events_2025_05 PARTITION OF shipment_tracking_events FOR VALUES FROM ('2025-05-01') TO ('2025-06-01');
CREATE TABLE shipment_tracking_events_2025_06 PARTITION OF shipment_tracking_events FOR VALUES FROM ('2025-06-01') TO ('2025-07-01');
CREATE TABLE shipment_tracking_events_2025_07 PARTITION OF shipment_tracking_events FOR VALUES FROM ('2025-07-01') TO ('2025-08-01');
CREATE TABLE shipment_tracking_events_2025_08 PARTITION OF shipment_tracking_events FOR VALUES FROM ('2025-08-01') TO ('2025-09-01');
CREATE TABLE shipment_tracking_events_2025_09 PARTITION OF shipment_tracking_events FOR VALUES FROM ('2025-09-01') TO ('2025-10-01');
CREATE TABLE shipment_tracking_events_2025_10 PARTITION OF shipment_tracking_events FOR VALUES FROM ('2025-10-01') TO ('2025-11-01');
CREATE TABLE shipment_tracking_events_2025_11 PARTITION OF shipment_tracking_events FOR VALUES FROM ('2025-11-01') TO ('2025-12-01');
CREATE TABLE shipment_tracking_events_2025_12 PARTITION OF shipment_tracking_events FOR VALUES FROM ('2025-12-01') TO ('2026-01-01');

CREATE INDEX idx_tracking_shipment ON shipment_tracking_events(shipment_id, event_timestamp DESC);
CREATE INDEX idx_tracking_external ON shipment_tracking_events(external_event_id) 
    WHERE external_event_id IS NOT NULL;

-- Prevent duplicate external events
CREATE UNIQUE INDEX idx_tracking_external_unique 
    ON shipment_tracking_events(shipment_id, external_event_id, event_timestamp)
    WHERE external_event_id IS NOT NULL;
