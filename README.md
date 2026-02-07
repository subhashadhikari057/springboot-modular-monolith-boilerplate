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

## Useful Commands

```sh
# Stop infra
docker compose down

# Reset infra (removes volumes)
docker compose down -v

# Tail logs
docker compose logs -f
```
