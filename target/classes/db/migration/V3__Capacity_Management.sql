-- 8. capacity_bookings
-- Track vessel/flight/truck capacity to prevent overbooking

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

-- Trigger for version increment
CREATE TRIGGER capacity_bookings_version_trigger
BEFORE UPDATE ON capacity_bookings
FOR EACH ROW
EXECUTE FUNCTION increment_version();

-- 9. shipment_reservations
-- Link shipments to capacity bookings

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
