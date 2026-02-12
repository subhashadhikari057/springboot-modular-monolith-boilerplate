# ADR 0005: Flyway Schema Governance

## Status
Accepted

## Context
The project requires deterministic schema evolution and environment consistency.

## Decision
Use Flyway migrations as the source of truth for database schema.  
Keep JPA schema mode as `validate` to detect mapping drift at startup.

## Alternatives Considered
1. Hibernate auto-DDL (`update/create`)
2. Manual SQL changes outside versioned migrations

## Consequences/Tradeoffs
- Pros: traceable, repeatable schema changes and safer deployments.
- Cons: more upfront migration effort and strict discipline required for every schema change.

