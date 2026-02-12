# Engineering Standards



## Purpose
This document is an implementation guide.  
Use it as the mandatory checklist for design, coding, review, and release decisions.

## Engineering Pillars (Mandatory)
1. SOLID design
2. Clean architecture boundaries
3. Configuration discipline (12-factor style)
4. API contract standards
5. Security baseline (OWASP mindset)
6. Data and migration governance
7. Resilience patterns
8. Performance and scalability discipline
9. Observability requirements
10. Testing strategy
11. Delivery governance
12. Code governance

## 1) SOLID Design Rules
- Keep each class focused on one reason to change.
- Use composition and strategies for extensibility.
- Avoid inheritance unless substitutability is explicit and tested.
- Keep interfaces small and role-specific.
- Depend on abstractions for cross-module and external integrations.
- Prevent God classes and utility dumping grounds.

## 2) Architecture Boundary Rules
Required dependency flow:
- `api -> application -> domain -> infrastructure`

Rules:
- `api`: transport concerns only.
- `application`: use-case orchestration and transaction boundaries.
- `domain`: business state and rules with minimal framework coupling.
- `infrastructure`: database, cache, broker, storage, and external APIs.
- Do not let infrastructure concerns leak into controllers or domain models.

## 3) Configuration Rules
- Keep config environment-driven.
- Never commit secrets.
- Use separate profiles for `dev`, `test`, and `prod`.
- Keep production defaults secure and explicit.

## 4) API Rules
- Use DTOs for request/response boundaries.
- Keep validation at boundary (`api`) layer.
- Use consistent pagination/filter/sort conventions.
- Use one consistent error response contract.
- Preserve backward compatibility for public API changes.

## 5) Security Rules
- Enforce authentication and authorization by default.
- Apply least privilege for roles and permissions.
- Keep cookie/token settings secure in production.
- Review endpoints against OWASP API risks.
- Log security-relevant actions for auditability.

## 6) Data and Migration Rules
- Flyway is the schema source of truth.
- Never modify applied migrations in shared environments.
- Every schema change requires a new migration.
- Prefer additive and backward-compatible migration steps.
- Enforce constraints and indexes based on data invariants.

## 7) Resilience Rules
- Set timeouts on external calls.
- Retry only idempotent/safe operations.
- Add fallback behavior for cache and broker outages.
- Avoid tight coupling between request path and non-critical dependencies.

## 8) Performance and Scalability Rules
- Define cache ownership per module.
- Define key, TTL, and invalidation strategy before caching.
- Avoid blocking Redis key scans in hot paths.
- Profile query paths and optimize indexes before scaling hardware.
- Load test critical endpoints before major releases.

## 9) Observability Rules
- Emit structured logs.
- Include request correlation IDs.
- Publish metrics for latency, error rate, throughput, and dependency health.
- Add traces for critical cross-component flows.
- Maintain alert thresholds and runbooks.



## Change Flow (Required)
For any non-trivial feature/refactor:
1. Define problem and acceptance criteria.
2. Confirm architecture boundaries and affected modules.
3. Record architecture-impacting decisions as ADR.
4. Design schema/cache/contracts before implementation.
5. Implement with tests.
6. Validate performance/security implications.
7. Update docs and PR checklist.

## ADR Standard
Store ADR files under:
- `docs/adr/`

Each ADR must include:
- Context/problem
- Decision
- Alternatives considered
- Consequences/tradeoffs
- Status (`Proposed`, `Accepted`, `Deprecated`, `Superseded`)

## PR Gate (Merge Criteria)
A PR is merge-ready only when all are true:
- Build passes.
- Required tests pass or documented exception is approved.
- Schema changes include migration.
- Security impact is reviewed.
- Backward compatibility impact is declared.
- Docs are updated where behavior or architecture changed.
