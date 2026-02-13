-- Consolidated auth baseline for fresh databases only.
-- Source parity: V1, V2, V3, V5, V6, V7 (auth/user scope only; upload excluded).

-- Extensions
CREATE SCHEMA IF NOT EXISTS extensions;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pgcrypto') THEN
        ALTER EXTENSION pgcrypto SET SCHEMA extensions;
    END IF;
END $$;

-- Core RBAC
CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE permissions (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE role_permissions (
    role_id INTEGER NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id INTEGER NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- Enums
CREATE TYPE user_status AS ENUM ('ACTIVE', 'DISABLED', 'LOCKED');

CREATE TYPE verification_purpose AS ENUM (
    'EMAIL_VERIFICATION',
    'PASSWORD_RESET',
    'PHONE_VERIFICATION',
    'ACCOUNT_DELETION'
);

CREATE TYPE verification_channel AS ENUM (
    'EMAIL',
    'SMS'
);

-- Users/Auth tables
CREATE TABLE users (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE,
    email_verified BOOLEAN NOT NULL DEFAULT false,
    phone TEXT,
    phone_verified BOOLEAN NOT NULL DEFAULT false,
    image TEXT,
    status user_status NOT NULL DEFAULT 'ACTIVE',
    role_id INTEGER REFERENCES roles(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX users_phone_unique_idx
    ON users (phone)
    WHERE phone IS NOT NULL;

CREATE INDEX users_role_id_idx
    ON users (role_id);

CREATE INDEX users_email_verified_idx
    ON users (email_verified);

CREATE INDEX users_created_at_desc_idx
    ON users (created_at DESC);

CREATE INDEX users_updated_at_desc_idx
    ON users (updated_at DESC);

CREATE INDEX users_role_email_verified_created_at_desc_idx
    ON users (role_id, email_verified, created_at DESC);

CREATE INDEX users_status_idx
    ON users (status);

CREATE TABLE sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token TEXT NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    refresh_token TEXT NOT NULL UNIQUE,
    refresh_expires_at TIMESTAMPTZ NOT NULL,
    ip_address TEXT,
    user_agent TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX sessions_user_id_idx ON sessions (user_id);

CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider_id TEXT NOT NULL,
    account_id TEXT NOT NULL,
    access_token TEXT,
    refresh_token TEXT,
    id_token TEXT,
    access_token_expires_at TIMESTAMPTZ,
    refresh_token_expires_at TIMESTAMPTZ,
    scope TEXT,
    password_hash TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (provider_id, account_id)
);

CREATE INDEX accounts_user_id_idx ON accounts (user_id);

CREATE INDEX accounts_user_provider_idx
    ON accounts (user_id, provider_id);

CREATE TABLE verifications (
    id UUID PRIMARY KEY,
    identifier TEXT NOT NULL,
    target TEXT NOT NULL,
    purpose verification_purpose NOT NULL,
    channel verification_channel NOT NULL,
    token_hash TEXT NOT NULL,
    otp_hash TEXT,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    metadata JSONB,
    attempts INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 5,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX verification_identifier_purpose_idx
    ON verifications (identifier, purpose);

CREATE INDEX verification_target_purpose_idx
    ON verifications (target, purpose);

CREATE INDEX verifications_active_lookup_idx
    ON verifications (identifier, purpose, channel, token_hash, expires_at)
    WHERE consumed_at IS NULL;

-- Seed roles
INSERT INTO roles (name, description)
VALUES
    ('SUPERADMIN', 'Full access to all system capabilities'),
    ('ADMIN', 'Administrative access to manage users and settings'),
    ('USER', 'Standard application user');

-- Seed permissions
INSERT INTO permissions (name, description)
VALUES
    ('users:create', 'Create users'),
    ('users:read', 'Read users'),
    ('users:delete', 'Delete users'),
    ('users:manage', 'Manage user role/status/permissions/password-reset actions'),
    ('users:update', 'Update user profile fields'),
    ('roles:create', 'Create roles'),
    ('roles:read', 'Read roles'),
    ('roles:manage', 'Update role permissions'),
    ('permissions:create', 'Create permissions'),
    ('permissions:read', 'Read permissions');

-- SUPERADMIN gets all current permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON true
WHERE r.name = 'SUPERADMIN';

-- ADMIN gets selected user-management permissions from previous migrations
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN (
    'users:update',
    'users:manage'
)
WHERE r.name = 'ADMIN';

-- Seed superadmin user/account
INSERT INTO users (id, name, email, email_verified, phone_verified, status, role_id)
VALUES (
    'c6bcbf33-60e2-4f1e-a30c-8e25d85f6e2f',
    'Super Admin',
    'superadmin@gmail.com',
    true,
    false,
    'ACTIVE',
    (SELECT id FROM roles WHERE name = 'SUPERADMIN')
);

INSERT INTO users (id, name, email, email_verified, phone_verified, status, role_id)
VALUES (
    '9b57c0cf-79d7-4c6e-8d84-9d3c2c4e56a1',
    'Admin User',
    'admin@gmail.com',
    true,
    false,
    'ACTIVE',
    (SELECT id FROM roles WHERE name = 'ADMIN')
);

INSERT INTO users (id, name, email, email_verified, phone_verified, status, role_id)
VALUES (
    '24e4a4fe-40c8-49ef-acd5-5d2df0d5f8cb',
    'Normal User',
    'user@gmail.com',
    true,
    false,
    'ACTIVE',
    (SELECT id FROM roles WHERE name = 'USER')
);

INSERT INTO accounts (id, user_id, provider_id, account_id, password_hash)
VALUES (
    'f5ef8d31-b3e7-4a03-a4cf-e8cc6b7f84fb',
    (SELECT id FROM users WHERE email = 'superadmin@gmail.com'),
    'local',
    'superadmin@gmail.com',
    extensions.crypt('superadmin123', extensions.gen_salt('bf', 10))
);

INSERT INTO accounts (id, user_id, provider_id, account_id, password_hash)
VALUES (
    'f78dbe2d-2d00-4b08-9be9-23c0f46ca8cf',
    (SELECT id FROM users WHERE email = 'admin@gmail.com'),
    'local',
    'admin@gmail.com',
    extensions.crypt('admin123', extensions.gen_salt('bf', 10))
);

INSERT INTO accounts (id, user_id, provider_id, account_id, password_hash)
VALUES (
    '2f763d80-626a-4d5e-a8e7-5d27a88f38d3',
    (SELECT id FROM users WHERE email = 'user@gmail.com'),
    'local',
    'user@gmail.com',
    extensions.crypt('user123', extensions.gen_salt('bf', 10))
);
