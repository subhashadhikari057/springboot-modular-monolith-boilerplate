# Backend Starter (Spring Boot)

A Spring Boot 3.x backend with Postgres, Redis, RabbitMQ, and Mailpit wired via Docker Compose.

## Prerequisites

- Java 17+
- Docker + Docker Compose
- (Optional) `psql`, `redis-cli`

## Quick Start

1) Copy env template

```sh
cp .env.example .env
```

2) Start infra

```sh
docker compose up -d
```

3) Run the app

```sh
./scripts/run-dev.sh
```

4) Verify

```sh
curl http://localhost:8080/health
```

## Services

All services are started by `docker-compose.yml`:

- Postgres: `localhost:5433`
  - DB: `app`
  - User: `app`
  - Pass: `app`
- Redis: `localhost:6380`
- RabbitMQ (AMQP): `localhost:5672`
- RabbitMQ UI: `http://localhost:15672` (user `app`, pass `app`)
- Mailpit SMTP: `localhost:1025`
- Mailpit UI: `http://localhost:8025`
- RedisInsight UI: `http://localhost:8001`

## Environment Variables

The app reads config from `.env` (loaded by `scripts/run-dev.sh`).

```ini
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
```

If you run from IntelliJ, add these env vars to the Run Configuration.

## RedisInsight Connection

In the RedisInsight UI, add a database with:

- Connection string (preferred): `redis://backend-redis:6379`

If that fails, use:

- `redis://host.docker.internal:6380`

## Health Check

- `GET /health` returns `ok` (no auth required).
- On app startup, a DB health check logs `DB health check OK` if the connection succeeds.

## Common Issues

### Datasource URL must start with "jdbc"
Make sure `SPRING_DATASOURCE_URL` includes `jdbc:`, e.g.

```
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/app
```

### Login page shows up
Spring Security is enabled by default. `/health` is open; other endpoints require auth.

### RedisInsight not loading
Restart it:

```sh
docker compose up -d --force-recreate redisinsight
```

Then open `http://localhost:8001`.

## Useful Commands

```sh
# Stop infra

docker compose down

# Reset infra (removes volumes)

docker compose down -v

# Tail logs

docker compose logs -f
```
