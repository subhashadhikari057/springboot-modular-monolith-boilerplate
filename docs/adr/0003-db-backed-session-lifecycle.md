# ADR 0003: DB-Backed Session Lifecycle

## Status
Accepted

## Context
Sessions need durable storage, refresh rotation, and explicit revocation on security events (logout/password changes).

## Decision
Persist sessions in PostgreSQL (`sessions` table), including access token, refresh token, and expiries.  
On refresh, rotate session by replacing prior session record with a new one.

## Alternatives Considered
1. Redis-only session storage
2. Stateless access/refresh JWT pair with no server session table

## Consequences/Tradeoffs
- Pros: durable session audit trail, deterministic revocation behavior.
- Cons: request-time DB dependency for session resolution unless optimized with cache.

