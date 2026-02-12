# ADR 0004: RBAC Authorization Model

## Status
Accepted

## Context
The system needs controlled access for user, role, permission, and upload operations with admin-level governance.

## Decision
Adopt RBAC with persistent entities:
- `roles`
- `permissions`
- `role_permissions`
- `users.role_id`

Use permission strings in method-level guards via `@PreAuthorize("hasAuthority('...')")`.

## Alternatives Considered
1. Role-only checks without granular permissions
2. Hardcoded endpoint rules without DB-modeled permissions

## Consequences/Tradeoffs
- Pros: fine-grained authorization, policy changes possible through data changes.
- Cons: permission model and seed data require governance and migration discipline.

