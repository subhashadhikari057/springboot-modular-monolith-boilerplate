CREATE TYPE user_status AS ENUM ('ACTIVE', 'DISABLED', 'LOCKED');

ALTER TABLE users
    ADD COLUMN status user_status NOT NULL DEFAULT 'ACTIVE';

CREATE INDEX users_status_idx ON users (status);

INSERT INTO permissions (name, description)
VALUES
    ('user:update', 'Update user profile fields'),
    ('user:update-status', 'Update user account status'),
    ('user:read-permissions', 'Read user effective permissions'),
    ('user:reset-password-request', 'Trigger password reset request for user')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN (
    'user:update',
    'user:update-status',
    'user:read-permissions',
    'user:reset-password-request'
)
WHERE r.name IN ('SUPERADMIN', 'ADMIN')
ON CONFLICT DO NOTHING;
