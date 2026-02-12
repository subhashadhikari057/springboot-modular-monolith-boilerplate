# Database Schema (Human Reference)

This folder contains human-readable documentation for the database schema.
The source of truth is Flyway migrations under `src/main/resources/db/migration/`.

## Migrations

- `V1__core_schema.sql`
  - Core schema: roles/permissions, users/accounts/sessions, verifications
  - Includes refresh-token columns in `sessions`

- `V2__seed_rbac_and_superadmin.sql`
  - Seeds default roles and permissions
  - Maps all permissions to `SUPERADMIN`
  - Seeds superadmin account (`superadmin@gmail.com`)

- `V3__move_pgcrypto_to_extensions_schema.sql`
  - Moves `pgcrypto` extension objects out of `public` into `extensions`
  - Keeps app tables list cleaner in DB tools

- `V4__uploads_module.sql`
  - Adds `uploads` table and indexes
  - Seeds `upload:create`, `upload:read`, `upload:delete` permissions
  - Grants upload permissions to `SUPERADMIN` and `ADMIN`

## Table Overview

- `roles`
  - `id` (serial), `name`, `description`, timestamps
- `permissions`
  - `id` (serial), `name`, `description`, timestamps
- `role_permissions`
  - `role_id`, `permission_id` (many-to-many)
- `users`
  - UUID, profile fields, role, verification flags, timestamps
- `sessions`
  - UUID, `user_id`, access token + refresh token, expiry fields, client metadata, timestamps
- `accounts`
  - UUID, `user_id`, provider/account ids, token fields, timestamps
- `verifications`
  - UUID, purpose/channel enums, token hashes, metadata, timestamps
- `uploads`
  - UUID, file metadata, local storage path/url, optional uploader id, timestamp

## Notes

- Migrations are append-only; do not edit applied migrations.
- Use new migrations for changes (e.g., `V4__...`).
