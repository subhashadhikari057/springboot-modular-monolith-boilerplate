# ADR 0002: Cookie Session Auth Model

## Status
Accepted

## Context
The backend requires authenticated browser/API access with server-side revocation and controlled session lifecycle.

## Decision
Use cookie-based authentication with:
- access cookie: `sid`
- refresh cookie: `rid`

Authentication is resolved server-side by session lookup and method-level permission checks.

## Alternatives Considered
1. Stateless JWT-only access tokens
2. Header-based opaque tokens without cookies

## Consequences/Tradeoffs
- Pros: server-side revocation and rotation control, cookie ergonomics for browser clients.
- Cons: server state required, additional complexity for cookie/security settings per environment.

