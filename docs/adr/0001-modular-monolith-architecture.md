# ADR 0001: Modular Monolith Architecture

## Status
Accepted

## Context
The project needs clear feature boundaries while remaining simple to develop and deploy as one backend service.

## Decision
Use a modular monolith structure with feature packages under `modules/*` and layered responsibilities:
- `api`
- `application`
- `domain`
- `infrastructure`

Shared cross-cutting concerns live under `common` and `config`.

## Alternatives Considered
1. Layer-first package structure across the whole app (`controller/service/repository`)
2. Microservices from day one

## Consequences/Tradeoffs
- Pros: simpler deployment, strong internal boundaries, easier incremental evolution.
- Cons: requires discipline to prevent cross-module coupling; large modules can still become monolithic internally.

