# Backend Starter (Spring Boot)

<p align="center">
  <img src="assets/spring.svg" width="60" alt="Spring Boot" style="margin-right: 5px;" />
  <img src="assets/postgres.svg" width="60" alt="PostgreSQL" style="margin-right: 5px;" />
  <img src="assets/redis.svg" width="60" alt="Redis" style="margin-right: 5px;" />
  <img src="assets/rabbit.svg" width="60" alt="RabbitMQ" style="margin-right: 5px;" />
  <img src="assets/mailpit.svg" width="60" alt="Mailpit" style="margin-right: 5px;" />
  <img src="assets/docker.svg" width="60" alt="Docker" />
</p>


A production-leaning Spring Boot 3.x backend with Postgres, Redis, RabbitMQ, and Mailpit wired via Docker Compose.

## Highlights

- Spring Boot 3.x baseline for APIs and services
- Local infra via Docker Compose
- Health endpoint at `/health`
- Mail capture with Mailpit UI
- Split API docs UI:
  - Admin: `http://localhost:8080/api-docs/admin`
  - Mobile: `http://localhost:8080/api-docs/mobile`

## Stack

- Java 17+
- Spring Boot 3.x
- Postgres, Redis, RabbitMQ
- Mailpit
- Docker + Docker Compose

## Quick Start

1. Copy env template

```sh
cp .env.example .env
```

2. Start infra

```sh
docker compose up -d
```

3. Run the app

```sh
./scripts/run-dev.sh
```

4. Verify

```sh
curl http://localhost:8080/health
```

5. Open API docs

```text
http://localhost:8080/api-docs/admin
http://localhost:8080/api-docs/mobile
```

## Services (Local)

All services are started by `docker-compose.yml`.

| Service | Host | Notes |
| --- | --- | --- |
| Postgres | `localhost:5433` | DB `app`, user `app`, pass `app` |
| Redis | `localhost:6380` | - |
| RabbitMQ (AMQP) | `localhost:5672` | user `app`, pass `app` |
| RabbitMQ UI | `http://localhost:15672` | user `app`, pass `app` |
| Mailpit SMTP | `localhost:1025` | - |
| Mailpit UI | `http://localhost:8025` | - |
| RedisInsight UI | `http://localhost:8001` | - |

## Environment Variables

The app reads config from `.env` (loaded by `scripts/run-dev.sh`).

```ini
PORT=8080
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/app
SPRING_DATASOURCE_USERNAME=app
SPRING_DATASOURCE_PASSWORD=app
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6380
SPRING_RABBITMQ_HOST=localhost
SPRING_RABBITMQ_PORT=5672
SPRING_RABBITMQ_USERNAME=app
SPRING_RABBITMQ_PASSWORD=app
SPRING_MAIL_HOST=localhost
SPRING_MAIL_PORT=1025
SPRING_MAIL_USERNAME=
SPRING_MAIL_PASSWORD=
AUTH_MAIL_FROM=no-reply@starterpack.local
AUTH_MAIL_VERIFICATION_LINK_BASE_URL=http://localhost:3000/verify
AUTH_MAIL_PASSWORD_RESET_LINK_BASE_URL=http://localhost:3000/reset-password
AUTH_VERIFICATION_EXPOSE_TOKEN_IN_RESPONSE=true
AUTH_VERIFICATION_RESEND_COOLDOWN=PT1M
RATE_LIMIT_ENABLED=true
RATE_LIMIT_PREFIX=rl
RATE_LIMIT_AUTH_LOGIN_WINDOW=PT1M
RATE_LIMIT_AUTH_LOGIN_MAX_REQUESTS=5
RATE_LIMIT_AUTH_LOGIN_BLOCK_DURATION=PT10M
AUDIT_RETENTION_ENABLED=true
AUDIT_RETENTION_DAYS=90
AUDIT_RETENTION_CRON='0 30 2 * * *'
AUDIT_RETENTION_ZONE=UTC
AUDIT_RETENTION_BATCH_SIZE=5000
```

If you run from IntelliJ, add these env vars to the Run Configuration.
Note: keep cron values quoted in `.env` because `scripts/run-dev.sh` sources it as a shell file.

## API Docs Routes

- Admin UI: `http://localhost:8080/api-docs/admin`
- Mobile UI: `http://localhost:8080/api-docs/mobile`
- Admin OpenAPI JSON: `http://localhost:8080/openapi/admin`
- Mobile OpenAPI JSON: `http://localhost:8080/openapi/mobile`

## RedisInsight Connection

Use one of these connection strings in the RedisInsight UI.

- `redis://backend-redis:6379`
- `redis://host.docker.internal:6380`

## Health Check

- `GET /health` returns `ok` without auth.
- On app startup, a DB health check logs `DB health check OK` if the connection succeeds.

## Common Issues

- Datasource URL must start with `jdbc:`
- If you see a login page, Spring Security is enabled by default. `/health` is open; other endpoints require auth.
- RedisInsight not loading. Restart it with `docker compose up -d --force-recreate redisinsight`.

## Docker Data Commands (Simple Order)

1. Start all services

```sh
docker compose up -d
```

2. Stop all services (keeps data)

```sh
docker compose down
```

3. Delete everything permanently (containers + networks + volumes)

```sh
docker compose down -v --remove-orphans
```

4. Reset only Postgres data (keep Redis/RabbitMQ data)

```sh
docker compose down
docker volume rm backend_pg_data
docker compose up -d postgres
```

5. Delete only Postgres data permanently

```sh
docker compose down
docker volume rm backend_pg_data
```

6. Start only Postgres

```sh
docker compose up -d postgres
```

If `backend_pg_data` is not found, run:

```sh
docker volume ls
```

Then use the exact Postgres volume name from your machine.

## Useful Commands

```sh
# Tail logs
docker compose logs -f
```
