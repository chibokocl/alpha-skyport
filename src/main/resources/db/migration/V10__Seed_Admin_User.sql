-- Seed the initial admin user
INSERT INTO admin_users (
    admin_id,
    email,
    password_hash,
    role,
    first_name,
    last_name,
    is_active,
    requires_2fa,
    created_at,
    updated_at
) VALUES (
    'cefa1cc0-69d4-4d82-8a69-7b0a93e87912',
    'admin@alphaskyport.com',
    '$2b$12$.8kbf7C6jfA61lf8jLB3reyKomGLLvorVymRQdvHCuh6n2vU9RmOa', -- 'admin123'
    'SUPER_ADMIN',
    'System',
    'Admin',
    true,
    false,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (email) DO NOTHING;
