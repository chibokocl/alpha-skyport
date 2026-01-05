-- V64__Seed_Tracking_Data.sql

-- 1. Ensure Countries exist (Upsert)
INSERT INTO countries (country_code, country_name, region, is_active)
VALUES 
    ('US', 'United States', 'North America', true),
    ('GB', 'United Kingdom', 'Europe', true),
    ('CN', 'China', 'Asia', true),
    ('DE', 'Germany', 'Europe', true)
ON CONFLICT (country_code) DO NOTHING;

-- Refresh materialized view for countries if needed (manual refresh usually)
REFRESH MATERIALIZED VIEW active_countries;

-- 2. Ensure Service exists
INSERT INTO freight_services (service_name, service_type, description, base_rate, estimated_days_min, estimated_days_max)
VALUES 
    ('Standard Air Freight', 'air', 'Reliable air cargo service', 5.50, 3, 5),
    ('Express Ocean', 'sea', 'Fast sea freight', 1.20, 15, 20)
ON CONFLICT (service_id) DO NOTHING; 
-- Note: serial ID might differ, we'll subselect below

-- 3. Create a Mock Customer User
INSERT INTO users (
    email, password_hash, user_type, first_name, last_name, company_name, 
    phone, country_code, is_verified, is_active
) VALUES (
    'customer@example.com', 
    '$2b$12$.8kbf7C6jfA61lf8jLB3reyKomGLLvorVymRQdvHCuh6n2vU9RmOa', -- 'admin123' (reused hash for simplicity)
    'business', 
    'John', 'Doe', 'Doe Logistics Ltd', 
    '+15550192834', 'US', true, true
) ON CONFLICT (email) DO NOTHING;

-- 4. Insert Mock Shipments
WITH customer AS (SELECT user_id FROM users WHERE email = 'customer@example.com' LIMIT 1),
     us_country AS (SELECT country_id FROM countries WHERE country_code = 'US' LIMIT 1),
     gb_country AS (SELECT country_id FROM countries WHERE country_code = 'GB' LIMIT 1),
     cn_country AS (SELECT country_id FROM countries WHERE country_code = 'CN' LIMIT 1),
     air_service AS (SELECT service_id FROM freight_services WHERE service_name = 'Standard Air Freight' LIMIT 1)

INSERT INTO shipments (
    tracking_number, user_id, service_id, 
    origin_country_id, destination_country_id, 
    origin_address, destination_address,
    cargo_description, cargo_weight, cargo_weight_unit,
    shipment_status, total_cost, payment_status,
    created_at, updated_at
) VALUES 
-- Shipment 1: In Transit (China -> US)
(
    'ASL-2026-0000001', (SELECT user_id FROM customer), (SELECT service_id FROM air_service),
    (SELECT country_id FROM cn_country), (SELECT country_id FROM us_country),
    '123 Industrial Park, Shanghai, CN', '456 Commerce Blvd, Los Angeles, CA, US',
    'Electronics Components', 150.00, 'kg',
    'in_transit', 1250.00, 'paid',
    NOW() - INTERVAL '5 days', NOW()
),
-- Shipment 2: Delivered (UK -> US)
(
    'ASL-2026-0000002', (SELECT user_id FROM customer), (SELECT service_id FROM air_service),
    (SELECT country_id FROM gb_country), (SELECT country_id FROM us_country),
    'London Heathrow Logistics Center', '789 Retail Ave, New York, NY, US',
    'Textile Samples', 45.50, 'kg',
    'delivered', 450.00, 'paid',
    NOW() - INTERVAL '10 days', NOW()
)
ON CONFLICT (tracking_number) DO NOTHING;

-- 5. Insert Tracking Events for these shipments
WITH shipment1 AS (SELECT shipment_id, created_at FROM shipments WHERE tracking_number = 'ASL-2026-0000001'),
     shipment2 AS (SELECT shipment_id, created_at FROM shipments WHERE tracking_number = 'ASL-2026-0000002')

INSERT INTO shipment_tracking_events (
    shipment_id, event_status, event_location, event_description, event_timestamp, event_source
) VALUES
-- Events for Shipment 1
((SELECT shipment_id FROM shipment1), 'confirmed', 'Shanghai', 'Shipment confirmed', (SELECT created_at FROM shipment1), 'system'),
((SELECT shipment_id FROM shipment1), 'picked_up', 'Shanghai', 'Picked up from sender', (SELECT created_at FROM shipment1) + INTERVAL '1 day', 'manual'),
((SELECT shipment_id FROM shipment1), 'in_transit', 'Shanghai Pudong Int. Airport', 'Departed origin country', (SELECT created_at FROM shipment1) + INTERVAL '2 days', 'system'),

-- Events for Shipment 2
((SELECT shipment_id FROM shipment2), 'confirmed', 'London', 'Shipment confirmed', (SELECT created_at FROM shipment2), 'system'),
((SELECT shipment_id FROM shipment2), 'picked_up', 'London', 'Picked up from sender', (SELECT created_at FROM shipment2) + INTERVAL '1 day', 'manual'),
((SELECT shipment_id FROM shipment2), 'in_transit', 'Heathrow Airport', 'Departed origin country', (SELECT created_at FROM shipment2) + INTERVAL '2 days', 'system'),
((SELECT shipment_id FROM shipment2), 'out_for_delivery', 'New York', 'Out for delivery', (SELECT created_at FROM shipment2) + INTERVAL '4 days', 'system'),
((SELECT shipment_id FROM shipment2), 'delivered', 'New York', 'Delivered to recipient', (SELECT created_at FROM shipment2) + INTERVAL '5 days', 'manual')
ON CONFLICT DO NOTHING;
