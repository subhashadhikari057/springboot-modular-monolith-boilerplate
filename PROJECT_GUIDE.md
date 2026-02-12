# Spring Boot + JPA + Flyway Project Guide

This guide is a complete technical walkthrough of this codebase. It is written for engineers who are new to the repository and need to understand not only what exists, but why it exists and how to extend it safely.

## 1) Project Overview & Architecture

### What this project is

This is a Spring Boot 3.5 backend starter with:

- REST APIs
- Cookie-based authentication (access + refresh token model)
- RBAC (roles and permissions)
- PostgreSQL (primary datastore)
- Flyway (schema and seed migrations)
- Redis, RabbitMQ, Mailpit infrastructure configured for local development
- Scalar API docs UI backed by OpenAPI

### High-level architecture

The project uses a **modular monolith** package design:

- Global cross-cutting concerns in `config/` and `common/`
- Feature modules in `modules/` (currently `auth` and `users`)
- Each module split into `api`, `application`, `domain`, `infrastructure`

Conceptually:

1. HTTP request enters a controller (`api`)
2. Controller delegates to service/use-case (`application`)
3. Service uses repositories (`infrastructure`) and entities (`domain`)
4. JPA/Hibernate translates entity operations to SQL
5. PostgreSQL persists and returns data
6. Response DTO is returned to client

### How Spring Boot, JPA, and Flyway work together

- **Spring Boot** wires the application context, auto-configuration, web server, security, and dependency injection.
- **Flyway** runs first at startup and ensures DB schema is at expected migration version.
- **JPA/Hibernate** starts after Flyway and validates mappings against schema (`ddl-auto: validate`).
- Runtime data access is JPA-driven through Spring Data repositories.

This order prevents schema drift and startup-time surprises.

### Request lifecycle (end-to-end)

Example: `POST /api/users`

1. Request hits `UserController` (`modules/users/api/UserController.java`)
2. `@Valid` validates request DTO
3. `@PreAuthorize` checks permission (`user:create`)
4. `UserService.createUser` applies business rules and normalizes input
5. `UserRepository` saves `User`, `AccountRepository` saves local account/password hash
6. Transaction commits
7. Controller returns `201` with `UserResponse`
8. Any exception is mapped by global error handlers to a consistent JSON error format

## 2) Project Structure & File-Level Explanation

## Root-level structure

- `pom.xml`: Maven build and dependency graph
- `docker-compose.yml`: local infra stack (Postgres, Redis, RabbitMQ, Mailpit, RedisInsight)
- `.env.example`: local env variable template
- `scripts/run-dev.sh`: loads `.env` and runs Spring Boot
- `schema/`: human-readable schema docs
- `src/main/resources/db/migration/`: Flyway source-of-truth migrations
- `src/main/java/com/starterpack/backend/`: application source

## Java package structure

- `com.starterpack.backend`
  - `BackendApplication.java`: bootstrap entrypoint
- `com.starterpack.backend.config`
  - framework/security/openapi/auth property configuration
- `com.starterpack.backend.common.error`
  - error response contract and centralized exception mapping
- `com.starterpack.backend.modules.auth`
  - authentication feature (cookie, sessions, password and verification flows)
- `com.starterpack.backend.modules.users`
  - user management and RBAC feature

## Important files and why they exist

### Core bootstrap

- `src/main/java/com/starterpack/backend/BackendApplication.java`
  - `@SpringBootApplication`: component scan + auto-config
  - `@ConfigurationPropertiesScan`: enables binding for classes like `AuthProperties`

### Configuration

- `src/main/resources/application.yaml`
  - central config for DB, JPA, Redis, RabbitMQ, Mail, OpenAPI/Scalar, auth token/cookie TTLs
- `src/main/java/com/starterpack/backend/config/SecurityConfig.java`
  - defines stateless security filter chain
  - permits selected public routes
  - registers custom auth filter and password encoder
  - enables method-level security (`@EnableMethodSecurity`)
- `src/main/java/com/starterpack/backend/config/SessionAuthenticationFilter.java`
  - reads access cookie (`sid`)
  - resolves active session by token + expiry
  - loads role/permissions into Spring Security authorities
- `src/main/java/com/starterpack/backend/config/AuthProperties.java`
  - typed binding for `auth.*` config (cookie settings and TTLs)
- `src/main/java/com/starterpack/backend/config/OpenApiConfig.java`
  - OpenAPI metadata (`Backend Boilerplate API`)
- `src/main/java/com/starterpack/backend/config/InfraInfoLogger.java`
  - startup runner that logs service endpoints and DB health ping
- `src/main/java/com/starterpack/backend/config/RestAuthenticationEntryPoint.java`
  - custom JSON 401 response format
- `src/main/java/com/starterpack/backend/config/RestAccessDeniedHandler.java`
  - custom JSON 403 response format

### Global error handling

- `src/main/java/com/starterpack/backend/common/error/ApiErrorResponse.java`
  - standard error payload contract (`timestamp`, `code`, `message`, `details`)
- `src/main/java/com/starterpack/backend/common/error/GlobalExceptionHandler.java`
  - centralized mapping for validation errors, conflict errors, malformed body, and fallback 500

### Auth module

- `src/main/java/com/starterpack/backend/modules/auth/api/AuthController.java`
  - all auth endpoints (`register`, `login`, `logout`, `me`, `refresh`, password, verification)
  - manages cookie extraction/setting via `AuthCookieService`
- `src/main/java/com/starterpack/backend/modules/auth/application/AuthService.java`
  - auth use-cases: register/login, session refresh rotation, password change/reset, verification
  - token hashing and generation
- `src/main/java/com/starterpack/backend/modules/auth/application/AuthCookieService.java`
  - writes/clears access and refresh cookies with shared settings
- `src/main/java/com/starterpack/backend/modules/auth/api/dto/*`
  - request/response contracts with validation and OpenAPI schema annotations

### Users + RBAC module

- `src/main/java/com/starterpack/backend/modules/users/api/UserController.java`
  - user CRUD-style operations and role assignment
  - permission-gated via `@PreAuthorize`
- `src/main/java/com/starterpack/backend/modules/users/api/RoleController.java`
  - role creation/listing/permission assignment
- `src/main/java/com/starterpack/backend/modules/users/api/PermissionController.java`
  - permission creation/listing
- `src/main/java/com/starterpack/backend/modules/users/application/*Service.java`
  - user/role/permission business logic
- `src/main/java/com/starterpack/backend/modules/users/domain/*.java`
  - JPA entities and enums (`User`, `Account`, `Session`, `Role`, `Permission`, `Verification`)
- `src/main/java/com/starterpack/backend/modules/users/infrastructure/*Repository.java`
  - Spring Data repositories with generated and derived query methods

### Migrations and schema docs

- `src/main/resources/db/migration/V1__core_schema.sql`
  - creates schema objects (tables, enums, indexes)
- `src/main/resources/db/migration/V2__seed_rbac_and_superadmin.sql`
  - seeds roles/permissions and superadmin account
- `src/main/resources/db/migration/V3__move_pgcrypto_to_extensions_schema.sql`
  - moves `pgcrypto` extension objects into `extensions` schema for cleaner DB tool views
- `schema/db/README.md`, `schema/modules/users.md`
  - human documentation of schema/module relationships

## Naming conventions and package strategy

- Package-by-feature (`modules/auth`, `modules/users`) for bounded contexts
- Layer-by-responsibility inside feature (`api`, `application`, `domain`, `infrastructure`)
- Request/response DTOs suffixed as `Request`/`Response`
- service classes suffixed as `Service`
- repository interfaces suffixed as `Repository`
- migration files use `V<version>__<description>.sql`

## 3) Spring Boot Core Concepts Used

### Startup flow

At boot:

1. `BackendApplication.main` starts Spring context
2. Config properties bind from `application.yaml` + env variables
3. DataSource is created
4. Flyway validates/applies migrations
5. JPA/Hibernate validates entity schema (`ddl-auto: validate`)
6. Security filter chain and custom beans initialize
7. Embedded Tomcat starts
8. `InfraInfoLogger` runs startup DB check and logs endpoints

### Configuration model

- Centralized in `application.yaml`
- Environment variables are injected via placeholders `${...}`
- `.env` loading is done by `scripts/run-dev.sh` for local execution

### Profiles

Currently no dedicated `application-dev.yaml`/`application-prod.yaml` split is present. Boot falls back to `default` profile unless `SPRING_PROFILES_ACTIVE` is set.

Recommended future split:

- `application-dev.yaml`
- `application-test.yaml`
- `application-prod.yaml`

### Dependency Injection and bean lifecycle

- Constructor injection is used across controllers/services/config beans
- Singleton beans are default scope
- Security and error handler beans are created once at startup
- `@ConfigurationProperties` bean (`AuthProperties`) is bound during context init

## 4) JPA & Hibernate Deep Dive

### Entity design and mappings

Core entities:

- `User` (`users`)
- `Account` (`accounts`)
- `Session` (`sessions`)
- `Role` (`roles`)
- `Permission` (`permissions`)
- `Verification` (`verifications`)

### Common annotations and purpose

- `@Entity`, `@Table`: maps class to table
- `@Id`, `@GeneratedValue`: primary key generation strategy
- `@Column`: explicit column settings and constraints
- `@ManyToOne`, `@OneToMany`, `@ManyToMany`: relationship mapping
- `@JoinColumn`, `@JoinTable`: FK and bridge table mapping
- `@CreationTimestamp`, `@UpdateTimestamp`: automatic audit timestamps
- `@Enumerated(EnumType.STRING)` + `@JdbcTypeCode(SqlTypes.NAMED_ENUM)`: PostgreSQL enum mapping
- `@JdbcTypeCode(SqlTypes.JSON)`: `jsonb` mapping for metadata

### Relationship model in domain

- `User -> Role`: many users to one role
- `User -> Session`: one user to many sessions
- `User -> Account`: one user to many accounts
- `Role <-> Permission`: many-to-many via `role_permissions`

### Repository layer

Spring Data repositories extend `JpaRepository`, giving:

- CRUD methods out-of-the-box
- query derivation by method name
- optional fetch tuning with `@EntityGraph`

Examples:

- `findByEmailIgnoreCase`
- `findByTokenAndExpiresAtAfter`
- `findByRefreshTokenAndRefreshExpiresAtAfter`

### Transaction management

- Services are annotated with `@Transactional`
- Read methods use `@Transactional(readOnly = true)` where applicable
- Write flows (register, create user, assign role, reset password) run in transactional boundaries

### Entity lifecycle and persistence context

- Entities loaded in a transaction are managed
- Changes are tracked and flushed automatically at commit
- Service methods often set fields without explicit `save` after initial load because managed entities are dirty-checked

### Runtime JPA behavior in this project

- Flyway owns schema DDL
- JPA validates schema shape (`validate`) and performs DML at runtime
- Lazy relationships are used for associations; specific fetch graphs are used where needed for auth/authorization checks

## 5) Flyway Migration Strategy

### Why Flyway is used here

- Version-controlled schema evolution
- deterministic startup behavior across environments
- auditability via `flyway_schema_history`

### Current migrations

- `V1__core_schema.sql`: schema creation
- `V2__seed_rbac_and_superadmin.sql`: baseline seed data
- `V3__move_pgcrypto_to_extensions_schema.sql`: extension object organization

### Naming convention

- `V<integer>__<snake_case_description>.sql`
- incremental and immutable once applied in shared environments

### Startup execution model

On app start Flyway:

1. Reads `flyway_schema_history`
2. Validates checksums of applied migrations
3. Applies unapplied versions in order
4. Updates history table

### Rollback strategy

Flyway Community is forward-only by default. Preferred approach:

- create a new corrective migration rather than modifying old applied files
- for local-only cleanup (before team adoption), reset DB and rebuild migration stack if needed

### Flyway + JPA coexistence rules

- Flyway creates/changes schema
- JPA `ddl-auto: validate` ensures mapping and schema stay aligned
- Never rely on JPA auto DDL in this project style

## 6) Database Design & Data Flow

### Schema design summary

Tables:

- `roles`, `permissions`, `role_permissions`
- `users`, `accounts`, `sessions`
- `verifications`
- `flyway_schema_history`

Important uniqueness constraints:

- `users.email` unique
- partial unique index on `users.phone` when non-null
- `accounts(provider_id, account_id)` unique
- `sessions.token` unique
- `sessions.refresh_token` unique
- `roles.name` and `permissions.name` unique

### Data flow example: login + refresh

1. `/api/auth/login` validates credentials
2. Service creates access token (`sid`) + refresh token (`rid`) in `sessions`
3. Cookies set in response
4. Access-protected request passes through `SessionAuthenticationFilter`
5. Filter loads session + role/permissions, sets authentication context
6. After access expiry, `/api/auth/refresh` uses `rid`, rotates session row, issues new cookies

### Migration-entity alignment

- Entity fields mirror migration columns
- Mismatch causes startup failure due to `ddl-auto: validate`
- Any entity structure change must be accompanied by migration update

## 7) `pom.xml` Detailed Breakdown

### Parent and language

- Spring Boot parent `3.5.10`
- Java 17

### Main dependencies and why

- `spring-boot-starter-web`: REST APIs
- `spring-boot-starter-security`: authn/authz
- `spring-boot-starter-validation`: Bean Validation
- `spring-boot-starter-data-jpa`: ORM and repositories
- `postgresql` (runtime): JDBC driver
- `flyway-core`, `flyway-database-postgresql`: migrations
- `springdoc-openapi-starter-webmvc-api`: OpenAPI generation
- `com.scalar.maven:scalar`: Scalar docs UI
- `spring-boot-starter-actuator`: observability and health support
- `spring-boot-starter-data-redis`: Redis integration readiness
- `spring-boot-starter-amqp`: RabbitMQ integration readiness
- `spring-boot-starter-websocket`: websocket readiness
- `lombok`: boilerplate reduction in entities

### Test dependencies

- `spring-boot-starter-test`
- `spring-rabbit-test`
- `spring-security-test`

### Build plugins

- `maven-compiler-plugin`: Lombok annotation processing
- `spring-boot-maven-plugin`: executable packaging and `spring-boot:run`

### Maven lifecycle impact

- `compile`: compiles and processes Lombok
- `test`: executes tests
- `package`: builds jar
- `spring-boot:run`: starts app from source/classpath

## 8) Error Handling, Validation, and Transactions

### Error strategy

- Services throw `ResponseStatusException` for business-level HTTP semantics
- `GlobalExceptionHandler` standardizes JSON payloads
- Security-specific 401/403 handled by dedicated handlers

### Validation strategy

- DTO-level constraints (`@NotBlank`, `@Email`, `@Size`, etc.)
- Controller methods use `@Valid`
- Violations return structured `VALIDATION_FAILED` responses

### Transaction boundaries and consistency

- Write flows wrapped in service-level transactions
- read-only transactions for query operations where marked
- password changes and reset flows invalidate sessions to maintain security consistency

## 9) Best Practices & Design Decisions

### Why these patterns

- Modular monolith structure keeps feature boundaries clear while staying single-deploy
- Flyway + JPA validate mode prevents schema drift
- Cookie-based session model supports server-side revocation and permission re-evaluation
- RBAC modeled in DB allows dynamic permission management
- DTOs isolate API contracts from entities

### Common pitfalls and mitigations

- **Pitfall:** changing entities without migration
  - **Mitigation:** `ddl-auto: validate` fails fast
- **Pitfall:** stale permissions in long-lived tokens
  - **Mitigation:** authorities loaded from DB-backed session lookup each request
- **Pitfall:** exposing generic HTML/login errors
  - **Mitigation:** custom JSON auth and access-denied handlers
- **Pitfall:** documentation UI broken by OpenAPI failure
  - **Mitigation:** OpenAPI endpoint exposed and validated (`/openapi.json`)

### Scalability and maintainability notes

- easy to add modules under `modules/`
- RBAC model can evolve to multi-role/user or tenant-scoped permissions later
- service boundaries are ready for extraction into separate services if needed

## 10) Run, Debug, and Extend the Project

### Local setup

1. Copy env file
   - `cp .env.example .env`
2. Start infrastructure
   - `docker compose up -d`
3. Run app
   - `./scripts/run-dev.sh`
4. Verify
   - `curl http://localhost:8080/health`
   - Docs UI: `http://localhost:8080/docs`
   - OpenAPI JSON: `http://localhost:8080/openapi.json`

### Debugging tips

- Run with IDE debugger and breakpoints in controllers/services
- Inspect cookies (`sid`, `rid`) during auth flows
- Check Flyway table:
  - `SELECT * FROM flyway_schema_history ORDER BY installed_rank;`
- For startup issues:
  - verify `.env` values (especially JDBC URL)
  - inspect `ddl-auto: validate` mismatch errors

### How to add a new feature safely

Use this order:

1. Design DB change
2. Add Flyway migration (`V<next>__...sql`)
3. Add/adjust entity mapping
4. Add repository methods if needed
5. Implement application service logic
6. Add controller endpoint + DTO validation + OpenAPI annotations
7. Add permission(s) and role mapping migration if endpoint should be protected
8. Compile and test endpoint behavior

### How to add new migrations safely

- Create a new version file, never rename old applied files in shared envs
- Keep migrations idempotent where appropriate for seed data (`ON CONFLICT DO NOTHING`)
- Prefer additive changes, then backfill data, then enforce constraints

### How to add new entities/APIs safely

- Keep entity names aligned with table names
- Use DTOs instead of exposing entities directly in API
- Add validation in request DTOs
- Add permission checks (`@PreAuthorize`) when endpoint is not public
- Return consistent errors through existing exception infrastructure

## API Surface Snapshot

### Auth endpoints

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/auth/me`
- `PATCH /api/auth/me`
- `POST /api/auth/refresh`
- `POST /api/auth/password/change`
- `POST /api/auth/verify/request`
- `POST /api/auth/verify/confirm`
- `POST /api/auth/password/forgot`
- `POST /api/auth/password/reset`

### User/RBAC endpoints

- `POST /api/users`
- `GET /api/users`
- `GET /api/users/{id}`
- `PUT /api/users/{id}/role`
- `POST /api/roles`
- `GET /api/roles`
- `PUT /api/roles/{id}/permissions`
- `POST /api/permissions`
- `GET /api/permissions`

### Public utility/docs endpoints

- `GET /health`
- `GET /openapi.json`
- `GET /docs`

## Final Notes

This codebase is intentionally set up so that schema management (Flyway), runtime persistence (JPA), auth/session security, and RBAC are explicit and inspectable. If you follow the extension workflow above, you can add features confidently without breaking consistency between code and database.
