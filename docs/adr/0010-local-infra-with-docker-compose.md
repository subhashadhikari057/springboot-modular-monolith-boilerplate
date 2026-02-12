# ADR 0010: Local Development Infrastructure with Docker Compose

## Status
Accepted

## Context
Developers need a reproducible local stack for backend dependencies (database, cache, broker, mail capture, inspection tools).

## Decision
Use Docker Compose as the local development infrastructure orchestrator for:
- PostgreSQL
- Redis
- RabbitMQ
- Mailpit
- RedisInsight

Mailpit is the local SMTP sink for auth flows (verification and password reset), allowing end-to-end testing without sending real internet email.

## Alternatives Considered
1. Manual local installation of each dependency
2. Remote shared development environment only

## Consequences/Tradeoffs
- Pros: fast onboarding and consistent local dependency behavior.
- Cons: compose setup is dev-focused and not a production deployment model.
