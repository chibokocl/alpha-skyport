-- V12__Seed_Demo_Data.sql

-- Helper CTEs to fetch IDs without hardcoding
WITH customer AS (SELECT user_id FROM users WHERE email = 'customer@example.com' LIMIT 1),
     us_country AS (SELECT country_id FROM countries WHERE country_code = 'US' LIMIT 1),
     gb_country AS (SELECT country_id FROM countries WHERE country_code = 'GB' LIMIT 1),
     de_country AS (SELECT country_id FROM countries WHERE country_code = 'DE' LIMIT 1),
     air_service AS (SELECT service_id FROM freight_services WHERE service_name = 'Standard Air Freight' LIMIT 1)

-- 1. Insert a PENDING Shipment (For testing 'Update Status')
INSERT INTO shipments (
    tracking_number, user_id, service_id, 
    origin_country_id, destination_country_id, 
    origin_address, destination_address,
    cargo_description, cargo_weight, cargo_weight_unit,
    shipment_status, total_cost, payment_status,
    created_at, updated_at
) 
SELECT 
    'ASL-2026-0000003', user_id, (SELECT service_id FROM air_service),
    (SELECT country_id FROM de_country), (SELECT country_id FROM us_country),
    'Berlin Tech Park, Building C', 'Tech Hub, Austin, TX, US',
    'Automotive Parts', 200.50, 'kg',
    'pending', 1800.00, 'paid',
    NOW(), NOW()
FROM customer
ON CONFLICT (tracking_number) DO NOTHING;

-- 2. Insert PENDING Quotes (For testing 'Approve/Reject')
WITH customer AS (SELECT user_id FROM users WHERE email = 'customer@example.com' LIMIT 1),
     us_country AS (SELECT country_id FROM countries WHERE country_code = 'US' LIMIT 1),
     cn_country AS (SELECT country_id FROM countries WHERE country_code = 'CN' LIMIT 1)

INSERT INTO quotes (
    user_id, origin_country_id, destination_country_id, service_id,
    cargo_description, cargo_weight, cargo_value,
    quote_status, quoted_price, created_at, valid_until
)
SELECT
    user_id, (SELECT country_id FROM cn_country), (SELECT country_id FROM us_country), NULL,
    'Urgent Medical Supplies', 50.00, 5000.00,
    'pending', 850.00, NOW(), NOW() + INTERVAL '7 days'
FROM customer;

WITH customer AS (SELECT user_id FROM users WHERE email = 'customer@example.com' LIMIT 1),
     gb_country AS (SELECT country_id FROM countries WHERE country_code = 'GB' LIMIT 1),
     de_country AS (SELECT country_id FROM countries WHERE country_code = 'DE' LIMIT 1)

INSERT INTO quotes (
    user_id, origin_country_id, destination_country_id, service_id,
    cargo_description, cargo_weight, cargo_value,
    quote_status, quoted_price, created_at, valid_until
)
SELECT
    user_id, (SELECT country_id FROM gb_country), (SELECT country_id FROM de_country), NULL,
    'Heavy Machinery Parts', 500.00, 15000.00,
    'pending', 2200.00, NOW(), NOW() + INTERVAL '7 days'
FROM customer;

-- 3. Insert an Issue for the In-Transit Shipment (For testing 'Resolve')
-- We link it to 'ASL-2026-0000001' which we know is 'in_transit' from V64
WITH shipment_target AS (SELECT shipment_id FROM shipments WHERE tracking_number = 'ASL-2026-0000001' LIMIT 1),
     admin_user AS (SELECT admin_id FROM admin_users LIMIT 1) -- Just pick first admin (likely the seeded one)

INSERT INTO shipment_issues (
    issue_id, shipment_id, issue_type, severity, title, 
    description, reported_by, status, created_at
)
SELECT
    gen_random_uuid(), shipment_id, 'customs', 'HIGH', 'Missing Commercial Invoice',
    'Customs clearance delayed due to missing documentation. Client contacted.',
    (SELECT admin_id FROM admin_user), 'OPEN', NOW()
FROM shipment_target
WHERE EXISTS (SELECT 1 FROM shipment_target);
