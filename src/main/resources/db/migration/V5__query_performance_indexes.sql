-- Users list/filter/sort performance
CREATE INDEX IF NOT EXISTS users_role_id_idx
    ON users (role_id);

CREATE INDEX IF NOT EXISTS users_email_verified_idx
    ON users (email_verified);

CREATE INDEX IF NOT EXISTS users_created_at_desc_idx
    ON users (created_at DESC);

CREATE INDEX IF NOT EXISTS users_updated_at_desc_idx
    ON users (updated_at DESC);

CREATE INDEX IF NOT EXISTS users_role_email_verified_created_at_desc_idx
    ON users (role_id, email_verified, created_at DESC);

-- Verification token lookup performance (active tokens only)
CREATE INDEX IF NOT EXISTS verifications_active_lookup_idx
    ON verifications (identifier, purpose, channel, token_hash, expires_at)
    WHERE consumed_at IS NULL;

-- Account lookup performance
CREATE INDEX IF NOT EXISTS accounts_user_provider_idx
    ON accounts (user_id, provider_id);
