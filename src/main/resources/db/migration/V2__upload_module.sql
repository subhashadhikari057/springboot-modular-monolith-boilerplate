-- Consolidated upload baseline for fresh databases only.
-- Requires V1__auth_core_baseline.sql (users/roles/permissions tables already present).

CREATE TABLE uploads (
    id UUID PRIMARY KEY,
    original_filename TEXT NOT NULL,
    stored_filename TEXT NOT NULL,
    content_type TEXT NOT NULL,
    extension TEXT NOT NULL,
    size_bytes BIGINT NOT NULL,
    storage_provider TEXT NOT NULL,
    storage_path TEXT NOT NULL UNIQUE,
    public_url TEXT NOT NULL,
    uploaded_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX uploads_uploaded_by_idx ON uploads (uploaded_by);
CREATE INDEX uploads_created_at_idx ON uploads (created_at);

INSERT INTO permissions (name, description)
VALUES
    ('uploads:create', 'Upload media files'),
    ('uploads:read', 'Read upload metadata'),
    ('uploads:delete', 'Delete uploaded media')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN ('uploads:create', 'uploads:read', 'uploads:delete')
WHERE r.name IN ('SUPERADMIN', 'ADMIN')
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name = 'uploads:create'
WHERE r.name = 'USER'
ON CONFLICT DO NOTHING;
