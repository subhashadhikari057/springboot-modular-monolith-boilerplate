# ADR 0008: Redis Cache Strategy

## Status
Accepted

## Context
The project needs lower latency for repeated read paths and a pattern to avoid unnecessary database load.

## Decision
Use Redis as a cache layer with module-owned keys and TTL-driven cache entries.  
Current implementation includes auth-session cache keys:
- `auth:sid:<sid>` for authenticated session context (role + permissions).
- `auth:rid:<rid>` for refresh-token lookup.
- `auth:user-sessions:<userId>` for bulk invalidation operations.

Invalidation rules:
- logout: remove sid/rid keys.
- refresh rotation: remove old sid/rid and write new keys.
- password change/reset: remove all user session keys.
- role/permission changes: immediate revocation of affected users' active sessions.

## Alternatives Considered
1. No caching (DB-only reads)
2. Shared global cache helper without module ownership

## Consequences/Tradeoffs
- Pros: reduced auth read latency and lower DB pressure for protected requests.
- Cons: cache invalidation complexity and operational dependency on Redis availability for best performance.
