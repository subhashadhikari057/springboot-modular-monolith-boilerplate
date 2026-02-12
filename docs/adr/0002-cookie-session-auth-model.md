# ADR 0002: Cookie Session Auth Model

## Status
Accepted

## Context
The backend requires authenticated browser/API access with server-side revocation and controlled session lifecycle.

## Decision
Use cookie-based authentication with:
- access cookie: `sid`
- refresh cookie: `rid`

Authentication is resolved server-side with Redis-first and DB-fallback session validation:
- `sid` is checked against Redis auth context cache first.
- On cache miss/unavailable cache, session is validated from PostgreSQL and cache is repopulated.
- Authorization uses role/permission authorities derived from the authenticated context.

Verification and reset token exposure policy:
- Development/local testing may expose plain verification/reset tokens in API responses.
- Production profile must disable token exposure in API responses (`auth.verification.expose-token-in-response=false`) and rely on email-delivered links/tokens.

## Alternatives Considered
1. Stateless JWT-only access tokens
2. Header-based opaque tokens without cookies

## Consequences/Tradeoffs
- Pros: server-side revocation and rotation control, cookie ergonomics for browser clients, faster warm-path auth checks through Redis.
- Cons: server state required, additional invalidation complexity, and cache consistency rules must be enforced.
