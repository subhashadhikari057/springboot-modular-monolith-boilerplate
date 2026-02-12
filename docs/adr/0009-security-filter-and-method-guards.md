# ADR 0009: Security Filter and Method-Level Authorization

## Status
Accepted

## Context
Authentication must be enforced on protected routes and authorization checks must remain explicit per operation.

## Decision
Use a custom `SessionAuthenticationFilter` to resolve session and authorities per request, combined with Spring method-level security via `@EnableMethodSecurity` and `@PreAuthorize`.

## Alternatives Considered
1. URL-only authorization config without method-level guards
2. Basic auth-only or form-login default security flow

## Consequences/Tradeoffs
- Pros: explicit per-use-case authorization and flexible endpoint policy control.
- Cons: requires careful authority mapping and testing to prevent authorization gaps.

