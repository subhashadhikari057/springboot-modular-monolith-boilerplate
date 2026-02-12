# ADR 0008: Redis Cache Strategy

## Status
Accepted

## Context
The project needs lower latency for repeated read paths and a pattern to avoid unnecessary database load.

## Decision
Use Redis as a cache layer with module-owned keys and TTL-driven cache entries.  
Current implementation targets user list caching in users module and invalidates list entries on write operations.

## Alternatives Considered
1. No caching (DB-only reads)
2. Shared global cache helper without module ownership

## Consequences/Tradeoffs
- Pros: reduced read latency and DB pressure for repeated queries.
- Cons: cache invalidation complexity; key strategy and invalidation approach must evolve for production-scale safety.

