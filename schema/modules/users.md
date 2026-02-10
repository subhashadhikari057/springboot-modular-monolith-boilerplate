# Users Module Schema

Purpose
- Identity, roles/permissions, sessions, external accounts, verifications

Tables
- `roles`
- `permissions`
- `role_permissions`
- `users`
- `sessions`
- `accounts`
- `verifications`

Key Relationships
- `users.role_id` -> `roles.id`
- `role_permissions.role_id` -> `roles.id`
- `role_permissions.permission_id` -> `permissions.id`
- `sessions.user_id` -> `users.id`
- `accounts.user_id` -> `users.id`
- `verifications` are standalone records tied by `identifier`

Invariants
- `users.email` is unique
- `accounts` unique on `(provider_id, account_id)`
- `roles.name` and `permissions.name` are unique

Migrations
- `V1__core_schema.sql`
- `V2__seed_rbac_and_superadmin.sql`
