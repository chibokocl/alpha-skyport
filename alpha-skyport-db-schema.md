# Alpha Skyport - PostgreSQL Database Schema v2 (Concurrency-Optimized)

## Database Overview
Enhanced schema with concurrency controls, optimistic locking, and scalability features for a high-traffic freight forwarding platform.

---

## Architecture Decisions

### Concurrency Strategy
- **Optimistic Locking**: Version columns on critical tables
- **Pessimistic Locking**: SELECT FOR UPDATE on financial operations
- **Idempotency Keys**: Prevent duplicate operations
- **Event Sourcing**: Immutable tracking events
- **Database Partitioning**: Time-based partitioning for large tables

### Scalability Features
- **Read Replicas**: Separate read-heavy queries
- **Caching Layer**: Redis for countries, services, pricing
- **Queue System**: Async processing for notifications, documents
- **Horizontal Sharding Ready**: User-based sharding capability

---

## Core Tables

### 1. users (Enhanced)
```sql
CREATE TABLE users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    user_type VARCHAR(20) NOT NULL CHECK (user_type IN ('private', 'business')),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    company_name VARCHAR(255),
    phone VARCHAR(50),
    country_code VARCHAR(3),
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state_province VARCHAR(100),
    postal_code VARCHAR(20),
    is_verified BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    
    -- Concurrency Control
    version INTEGER DEFAULT 1 NOT NULL,
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_type ON users(user_type);
CREATE INDEX idx_users_active ON users(is_active) WHERE is_active = TRUE;

-- Trigger for version increment
CREATE OR REPLACE FUNCTION increment_version()
RETURNS TRIGGER AS $$
BEGIN
    NEW.version = OLD.version + 1;
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER users_version_trigger
BEFORE UPDATE ON users
FOR EACH ROW
EXECUTE FUNCTION increment_version();
```

---

### 2. countries (Cached Reference Table)
```sql
CREATE TABLE countries (
    country_id SERIAL PRIMARY KEY,
    country_code VARCHAR(3) UNIQUE NOT NULL,
    country_name VARCHAR(100) NOT NULL,
    region VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    
    -- Cache invalidation
    cache_version INTEGER DEFAULT 1,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_countries_code ON countries(country_code);
CREATE INDEX idx_countries_active ON countries(is_active) WHERE is_active = TRUE;

-- Materialized view for fast lookups
CREATE MATERIALIZED VIEW active_countries AS
SELECT country_id, country_code, country_name, region
FROM countries
WHERE is_active = TRUE;

CREATE UNIQUE INDEX ON active_countries(country_code);
```

---

### 3. freight_services (Cached)
```sql
CREATE TABLE freight_services (
    service_id SERIAL PRIMARY KEY,
    service_name VARCHAR(100) NOT NULL,
    service_type VARCHAR(20) NOT NULL CHECK (service_type IN ('sea', 'air', 'land')),
    description TEXT,
    base_rate DECIMAL(10, 2),
    currency VARCHAR(3) DEFAULT 'USD',
    estimated_days_min INTEGER,
    estimated_days_max INTEGER,
    
    -- Capacity Management
    max_daily_capacity_kg DECIMAL(12, 2),
    max_daily_capacity_m3 DECIMAL(12, 2),
    
    is_active BOOLEAN DEFAULT TRUE,
    cache_version INTEGER DEFAULT 1,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_freight_service_type ON freight_services(service_type);
CREATE INDEX idx_freight_active ON freight_services(is_active) WHERE is_active = TRUE;
```

---

### 4. quotes (With Optimistic Locking)
```sql
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

CREATE TRIGGER quotes_version_trigger
BEFORE UPDATE ON quotes
FOR EACH ROW
EXECUTE FUNCTION increment_version();
```

---

### 5. tracking_number_sequence
**Atomic tracking number generation**

```sql
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
```

---

### 6. shipments (Enhanced with Locking)
```sql
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
```

---

### 7. shipment_tracking_events (Partitioned)
**Event sourcing pattern - immutable events**

```sql
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

-- Add more partitions as needed...

CREATE INDEX idx_tracking_shipment ON shipment_tracking_events(shipment_id, event_timestamp DESC);
CREATE INDEX idx_tracking_external ON shipment_tracking_events(external_event_id) 
    WHERE external_event_id IS NOT NULL;

-- Prevent duplicate external events
CREATE UNIQUE INDEX idx_tracking_external_unique 
    ON shipment_tracking_events(shipment_id, external_event_id)
    WHERE external_event_id IS NOT NULL;
```

---

### 8. capacity_bookings
**Track vessel/flight/truck capacity to prevent overbooking**

```sql
CREATE TABLE capacity_bookings (
    booking_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_id INTEGER NOT NULL REFERENCES freight_services(service_id),
    booking_date DATE NOT NULL,
    
    -- Capacity tracking
    reserved_weight_kg DECIMAL(12, 2) DEFAULT 0,
    reserved_volume_m3 DECIMAL(12, 2) DEFAULT 0,
    
    -- Limits (copied from service for this date)
    max_weight_kg DECIMAL(12, 2),
    max_volume_m3 DECIMAL(12, 2),
    
    -- Concurrency control
    version INTEGER DEFAULT 1 NOT NULL,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT capacity_weight_check CHECK (reserved_weight_kg <= max_weight_kg),
    CONSTRAINT capacity_volume_check CHECK (reserved_volume_m3 <= max_volume_m3),
    
    -- Unique per service per date
    UNIQUE(service_id, booking_date)
);

CREATE INDEX idx_capacity_service_date ON capacity_bookings(service_id, booking_date);

-- Function to atomically reserve capacity
CREATE OR REPLACE FUNCTION reserve_capacity(
    p_service_id INTEGER,
    p_booking_date DATE,
    p_weight_kg DECIMAL,
    p_volume_m3 DECIMAL
) RETURNS BOOLEAN AS $$
DECLARE
    available_weight DECIMAL;
    available_volume DECIMAL;
BEGIN
    -- Lock the row for this service/date
    SELECT (max_weight_kg - reserved_weight_kg),
           (max_volume_m3 - reserved_volume_m3)
    INTO available_weight, available_volume
    FROM capacity_bookings
    WHERE service_id = p_service_id AND booking_date = p_booking_date
    FOR UPDATE;
    
    -- Check if capacity available
    IF available_weight >= p_weight_kg AND available_volume >= p_volume_m3 THEN
        UPDATE capacity_bookings
        SET reserved_weight_kg = reserved_weight_kg + p_weight_kg,
            reserved_volume_m3 = reserved_volume_m3 + p_volume_m3,
            version = version + 1
        WHERE service_id = p_service_id AND booking_date = p_booking_date;
        
        RETURN TRUE;
    ELSE
        RETURN FALSE;
    END IF;
END;
$$ LANGUAGE plpgsql;
```

---

### 9. shipment_reservations
**Link shipments to capacity bookings**

```sql
CREATE TABLE shipment_reservations (
    reservation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shipment_id UUID NOT NULL REFERENCES shipments(shipment_id) ON DELETE CASCADE,
    booking_id UUID NOT NULL REFERENCES capacity_bookings(booking_id),
    
    reserved_weight_kg DECIMAL(12, 2) NOT NULL,
    reserved_volume_m3 DECIMAL(12, 2) NOT NULL,
    
    reservation_status VARCHAR(20) DEFAULT 'active' CHECK (reservation_status IN (
        'active', 'released', 'confirmed'
    )),
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    released_at TIMESTAMP,
    
    UNIQUE(shipment_id, booking_id)
);

CREATE INDEX idx_reservations_shipment ON shipment_reservations(shipment_id);
CREATE INDEX idx_reservations_booking ON shipment_reservations(booking_id);
```

---

### 10. payment_transactions
**Detailed payment tracking with idempotency**

```sql
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
```

---

### 11. notification_queue (Async Processing)
**Deduplication and retry logic**

```sql
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
```

---

### 12. audit_log
**Complete audit trail for compliance**

```sql
CREATE TABLE audit_log (
    audit_id BIGSERIAL PRIMARY KEY,
    
    -- What changed
    table_name VARCHAR(100) NOT NULL,
    record_id VARCHAR(100) NOT NULL,
    action VARCHAR(20) NOT NULL CHECK (action IN ('INSERT', 'UPDATE', 'DELETE')),
    
    -- Who made the change
    user_id UUID REFERENCES users(user_id),
    performed_by VARCHAR(100),
    ip_address INET,
    
    -- Change details
    old_values JSONB,
    new_values JSONB,
    changed_fields TEXT[],
    
    -- Context
    reason TEXT,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) PARTITION BY RANGE (created_at);

-- Monthly partitions
CREATE TABLE audit_log_2025_01 PARTITION OF audit_log
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');

CREATE INDEX idx_audit_table_record ON audit_log(table_name, record_id);
CREATE INDEX idx_audit_user ON audit_log(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_audit_created ON audit_log(created_at DESC);
```

---

## Concurrency Control Patterns

### Pattern 1: Optimistic Locking
```sql
-- Update with version check
UPDATE shipments
SET shipment_status = 'delivered',
    actual_delivery_date = CURRENT_DATE,
    version = version + 1
WHERE shipment_id = 'xxx'
  AND version = 5  -- Must match current version
RETURNING *;

-- If affected rows = 0, someone else updated it
```

### Pattern 2: Pessimistic Locking
```sql
BEGIN;

-- Lock the row
SELECT * FROM shipments
WHERE shipment_id = 'xxx'
FOR UPDATE;

-- Do your work
UPDATE shipments SET ...;

COMMIT;
```

### Pattern 3: Advisory Locks
```sql
-- Application-level locking
SELECT pg_advisory_lock(shipment_id::bigint);

-- Do work...

SELECT pg_advisory_unlock(shipment_id::bigint);
```

---

## Performance Optimizations

### 1. Connection Pooling
```sql
-- Use PgBouncer or similar
-- Pool size: (2 × CPU cores) + effective_spindle_count
```

### 2. Prepared Statements
```sql
PREPARE get_shipment AS
SELECT * FROM shipments WHERE tracking_number = $1;

EXECUTE get_shipment('ASL-2025-1000123');
```

### 3. Materialized Views for Reports
```sql
CREATE MATERIALIZED VIEW shipment_statistics AS
SELECT 
    DATE_TRUNC('day', created_at) as date,
    shipment_status,
    COUNT(*) as count,
    AVG(total_cost) as avg_cost
FROM shipments
GROUP BY DATE_TRUNC('day', created_at), shipment_status;

CREATE UNIQUE INDEX ON shipment_statistics(date, shipment_status);

-- Refresh periodically
REFRESH MATERIALIZED VIEW CONCURRENTLY shipment_statistics;
```

### 4. Batch Operations
```sql
-- Insert multiple tracking events atomically
INSERT INTO shipment_tracking_events (shipment_id, event_status, event_timestamp)
SELECT shipment_id, 'in_transit', CURRENT_TIMESTAMP
FROM shipments
WHERE shipment_status = 'picked_up'
ON CONFLICT (shipment_id, external_event_id) DO NOTHING;
```

---

## Critical Configuration

### PostgreSQL Settings for Concurrency
```ini
# postgresql.conf
max_connections = 200
shared_buffers = 4GB
effective_cache_size = 12GB
maintenance_work_mem = 1GB
checkpoint_completion_target = 0.9
wal_buffers = 16MB
default_statistics_target = 100
random_page_cost = 1.1
effective_io_concurrency = 200
work_mem = 20MB
min_wal_size = 2GB
max_wal_size = 8GB

# For high write throughput
synchronous_commit = off  # Careful with this
wal_writer_delay = 200ms

# Locking
deadlock_timeout = 1s
lock_timeout = 10s
statement_timeout = 30s
```

---

## Sample Concurrent Operations

### Safe Quote Conversion
```sql
CREATE OR REPLACE FUNCTION convert_quote_to_shipment(
    p_quote_id UUID,
    p_expected_version INTEGER
) RETURNS UUID AS $$
DECLARE
    v_shipment_id UUID;
    v_tracking_number VARCHAR(50);
BEGIN
    -- Generate tracking number atomically
    v_tracking_number := generate_tracking_number();
    
    -- Create shipment
    INSERT INTO shipments (
        tracking_number, user_id, quote_id, service_id,
        origin_country_id, destination_country_id, total_cost
    )
    SELECT 
        v_tracking_number, user_id, quote_id, service_id,
        origin_country_id, destination_country_id, quoted_price
    FROM quotes
    WHERE quote_id = p_quote_id
      AND version = p_expected_version
      AND quote_status = 'accepted'
      AND converted_to_shipment_id IS NULL
    RETURNING shipment_id INTO v_shipment_id;
    
    -- If insert failed, quote already converted or version mismatch
    IF v_shipment_id IS NULL THEN
        RAISE EXCEPTION 'Quote conversion failed - may be already converted or version mismatch';
    END IF;
    
    -- Mark quote as converted
    UPDATE quotes
    SET quote_status = 'converted',
        converted_to_shipment_id = v_shipment_id,
        converted_at = CURRENT_TIMESTAMP,
        version = version + 1
    WHERE quote_id = p_quote_id;
    
    -- Mark tracking number as used
    UPDATE tracking_numbers
    SET shipment_id = v_shipment_id,
        is_used = TRUE,
        used_at = CURRENT_TIMESTAMP
    WHERE tracking_number = v_tracking_number;
    
    RETURN v_shipment_id;
END;
$$ LANGUAGE plpgsql;
```

### Safe Payment Processing
```sql
CREATE OR REPLACE FUNCTION process_payment(
    p_shipment_id UUID,
    p_amount DECIMAL,
    p_idempotency_key VARCHAR
) RETURNS UUID AS $$
DECLARE
    v_transaction_id UUID;
    v_existing_transaction UUID;
BEGIN
    -- Check idempotency
    SELECT transaction_id INTO v_existing_transaction
    FROM payment_transactions
    WHERE idempotency_key = p_idempotency_key;
    
    IF v_existing_transaction IS NOT NULL THEN
        RETURN v_existing_transaction; -- Already processed
    END IF;
    
    -- Lock shipment row
    PERFORM 1 FROM shipments
    WHERE shipment_id = p_shipment_id
    FOR UPDATE;
    
    -- Create transaction
    INSERT INTO payment_transactions (
        shipment_id, idempotency_key, amount, 
        currency, transaction_type, transaction_status
    ) VALUES (
        p_shipment_id, p_idempotency_key, p_amount,
        'USD', 'payment', 'completed'
    ) RETURNING transaction_id INTO v_transaction_id;
    
    -- Update shipment payment status
    UPDATE shipments
    SET amount_paid = amount_paid + p_amount,
        amount_due = total_cost - (amount_paid + p_amount),
        payment_status = CASE
            WHEN (amount_paid + p_amount) >= total_cost THEN 'paid'
            WHEN (amount_paid + p_amount) > 0 THEN 'partial'
            ELSE 'unpaid'
        END,
        version = version + 1
    WHERE shipment_id = p_shipment_id;
    
    RETURN v_transaction_id;
END;
$$ LANGUAGE plpgsql;
```

---

## Monitoring Queries

### Check for Lock Contention
```sql
SELECT 
    blocking.pid AS blocking_pid,
    blocked.pid AS blocked_pid,
    blocking.query AS blocking_query,
    blocked.query AS blocked_query,
    blocked.wait_event_type,
    blocked.wait_event
FROM pg_stat_activity AS blocked
JOIN pg_stat_activity AS blocking 
    ON blocking.pid = ANY(pg_blocking_pids(blocked.pid))
WHERE blocked.wait_event IS NOT NULL;
```

### Track Table Bloat
```sql
SELECT 
    schemaname, tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size,
    n_dead_tup,
    n_live_tup,
    ROUND(n_dead_tup * 100.0 / NULLIF(n_live_tup + n