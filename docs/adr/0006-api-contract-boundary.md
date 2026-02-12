# ADR 0006: API Contract Boundary with DTOs

## Status
Accepted

## Context
Public API contracts must remain stable and independent from persistence entity shape.

## Decision
Use explicit request/response DTOs at controller boundaries.  
Use centralized exception handling for consistent error payloads.

Security boundary note:
- Verification and forgot-password DTO responses keep stable shape across environments.
- In production profile, token fields are intentionally not exposed and may be `null`; clients must use email link flows instead of response tokens.

## Alternatives Considered
1. Return JPA entities directly from controllers
2. Endpoint-specific ad hoc error response shapes

## Consequences/Tradeoffs
- Pros: decouples API from persistence, improves backward compatibility and validation clarity.
- Cons: requires mapping code and maintenance of DTO models, including environment-aware response field handling.
