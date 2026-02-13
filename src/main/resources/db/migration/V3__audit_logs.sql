-- Audit log baseline for security and operational event tracking.
-- Append-only design for admin investigation and user self-history queries.

CREATE TYPE audit_result AS ENUM (
    'SUCCESS',
    'FAILURE'
);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    actor_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    actor_email TEXT,
    action TEXT NOT NULL,
    resource_type TEXT,
    resource_id TEXT,
    result audit_result NOT NULL,
    reason_code TEXT,
    ip_address TEXT,
    user_agent TEXT,
    request_id TEXT,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX idx_audit_logs_occurred_at_desc
    ON audit_logs (occurred_at DESC);

CREATE INDEX idx_audit_logs_actor_user_occurred_at_desc
    ON audit_logs (actor_user_id, occurred_at DESC);

CREATE INDEX idx_audit_logs_action_occurred_at_desc
    ON audit_logs (action, occurred_at DESC);

CREATE INDEX idx_audit_logs_resource_lookup
    ON audit_logs (resource_type, resource_id, occurred_at DESC);

CREATE INDEX idx_audit_logs_result_occurred_at_desc
    ON audit_logs (result, occurred_at DESC);

CREATE INDEX idx_audit_logs_request_id
    ON audit_logs (request_id)
    WHERE request_id IS NOT NULL;
