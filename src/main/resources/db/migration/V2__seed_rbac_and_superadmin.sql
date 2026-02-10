CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO roles (name, description)
VALUES
    ('SUPERADMIN', 'Full access to all system capabilities'),
    ('ADMIN', 'Administrative access to manage users and settings'),
    ('USER', 'Standard application user')
ON CONFLICT (name) DO NOTHING;

INSERT INTO permissions (name, description)
VALUES
    ('user:create', 'Create users'),
    ('user:read', 'Read users'),
    ('user:update-role', 'Update user role'),
    ('role:create', 'Create roles'),
    ('role:read', 'Read roles'),
    ('role:update-permissions', 'Update role permissions'),
    ('permission:create', 'Create permissions'),
    ('permission:read', 'Read permissions')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON true
WHERE r.name = 'SUPERADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO users (id, name, email, email_verified, phone_verified, role_id)
VALUES (
    'c6bcbf33-60e2-4f1e-a30c-8e25d85f6e2f',
    'Super Admin',
    'superadmin@gmail.com',
    true,
    false,
    (SELECT id FROM roles WHERE name = 'SUPERADMIN')
)
ON CONFLICT (email) DO NOTHING;

INSERT INTO accounts (id, user_id, provider_id, account_id, password_hash)
VALUES (
    'f5ef8d31-b3e7-4a03-a4cf-e8cc6b7f84fb',
    (SELECT id FROM users WHERE email = 'superadmin@gmail.com'),
    'local',
    'superadmin@gmail.com',
    crypt('superadmin123', gen_salt('bf', 10))
)
ON CONFLICT (provider_id, account_id) DO NOTHING;

UPDATE accounts
SET password_hash = crypt('superadmin123', gen_salt('bf', 10))
WHERE provider_id = 'local'
  AND account_id = 'superadmin@gmail.com';
