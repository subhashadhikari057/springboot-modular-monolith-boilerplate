# ADR 0003: DB-Backed Session Lifecycle

## Status
Accepted

## Context
Sessions need durable storage, refresh rotation, and explicit revocation on security events (logout/password changes).

## Decision
Persist sessions in PostgreSQL (`sessions` table), including access token, refresh token, and expiries.  
On refresh, rotate session by replacing prior session record with a new one.

Redis is coupled as a cache (not source of truth):
- `auth:sid:<sid>` caches auth context with access TTL.
- `auth:rid:<rid>` caches refresh reference with refresh TTL.
- `auth:user-sessions:<userId>` tracks active session tokens for bulk invalidation.

## Alternatives Considered
1. Redis-only session storage
2. Stateless access/refresh JWT pair with no server session table

## Consequences/Tradeoffs
- Pros: durable session audit trail, deterministic revocation behavior, and lower read latency with Redis warm cache.
- Cons: invalidation logic must be kept correct during logout, refresh rotation, password changes, and role/permission changes.
