# TaskFlow — System Design Document

| Field       | Value                          |
|-------------|--------------------------------|
| **Version** | 1.0                            |
| **Status**  | Living Document                |
| **Author**  | Eric Kikunda                   |
| **Created** | 2026-05-27                     |
| **Updated** | 2026-05-27                     |

---

## Table of Contents

1. [Overview](#1-overview)
2. [Goals & Non-Goals](#2-goals--non-goals)
3. [Background & Motivation](#3-background--motivation)
4. [High-Level Architecture](#4-high-level-architecture)
5. [Component Design](#5-component-design)
6. [Data Model](#6-data-model)
7. [API Design](#7-api-design)
8. [Security Design](#8-security-design)
9. [Storage Design](#9-storage-design)
10. [Observability](#10-observability)
11. [Error Handling](#11-error-handling)
12. [Performance Considerations](#12-performance-considerations)
13. [Deployment](#13-deployment)
14. [Trade-offs & Alternatives Considered](#14-trade-offs--alternatives-considered)
15. [Scalability Limits & Future Work](#15-scalability-limits--future-work)
16. [Open Questions](#16-open-questions)
17. [Glossary](#17-glossary)

---

## 1. Overview

TaskFlow is a full-stack task and project management application. Users register, create projects, invite teammates, and track work items (tasks) through a defined lifecycle — from `TODO` through `IN_PROGRESS` to `DONE`. Each task supports threaded comments, priority levels, due dates, and assignee management.

The backend exposes a versioned REST API (Spring Boot 4, Java 21). The frontend is a single-page application (React 19, TypeScript, Vite). Authentication is stateless JWT; persistence is PostgreSQL in production and H2 in development.

**Scope of this document:** backend REST API and its data layer. Frontend internals are covered in the frontend repository's `SYSTEM_DESIGN.md`.

---

## 2. Goals & Non-Goals

### 2.1 Functional Goals

- Users can register and log in; sessions are stateless (JWT).
- Users can create projects and invite members by user ID.
- Project members can create, assign, update status, and delete tasks within their projects.
- Tasks support priority levels, due dates, and comments.
- Admins can manage users, change roles, and perform bulk project-status updates.
- All mutations are scoped to the authenticated user's project membership.

### 2.2 Non-Functional Goals

| Property        | Target                                                          |
|-----------------|-----------------------------------------------------------------|
| **Security**    | No unauthenticated access to resources; role + ownership checks on every mutation |
| **Correctness** | Task status transitions are enforced by a state machine, not ad-hoc checks |
| **Auditability**| Every request carries a correlation ID for log tracing         |
| **Operability** | Health and metrics endpoints exposed without auth              |
| **Testability** | Integration tests run against a real PostgreSQL container via Testcontainers |

### 2.3 Non-Goals (Explicit)

- **Real-time / WebSocket updates.** Clients poll; push notifications are out of scope.
- **File attachments.** No blob/S3 storage is provisioned.
- **Soft deletes / audit trail.** Deleted records are gone; no change history is maintained.
- **Email notifications.** User registration does not send a confirmation email.
- **Multi-tenancy isolation below the project level.** All data lives in one schema.
- **Rate limiting.** No per-IP or per-user throttle is implemented (see §15).

---

## 3. Background & Motivation

The common alternative to a custom tracker is a SaaS tool (Jira, Linear, Asana). The driver for building TaskFlow is educational: the project intentionally covers every layer of a production Spring Boot application — security, persistence, service logic, observability, and testing — in a single coherent codebase rather than isolated toy examples.

Design decisions are therefore biased toward production patterns (Flyway, Testcontainers, JWTs, RFC 7807 error bodies) over simplicity shortcuts (in-memory auth, auto-DDL, generic error strings).

---

## 4. High-Level Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                          Client (Browser)                        │
│         React 19 SPA — Vite dev server (port 5173)               │
│         Axios + JWT interceptors, React Router 7 nav             │
└───────────────────────────┬──────────────────────────────────────┘
                            │  HTTPS  (Authorization: Bearer <token>)
                            ▼
┌──────────────────────────────────────────────────────────────────┐
│                    Spring Boot API Server                        │
│                    Java 21  ·  port 8080                         │
│                                                                  │
│  ┌─────────────┐   ┌──────────────┐   ┌───────────────────────┐ │
│  │  Security   │   │  Controllers │   │  Services             │ │
│  │  Filter     │──▶│  /api/v1/**  │──▶│  Business Logic       │ │
│  │  Chain      │   │              │   │  State Machine        │ │
│  │  (JWT)      │   │  MapStruct   │   │  Authorization Rules  │ │
│  └─────────────┘   │  DTO mapping │   └──────────┬────────────┘ │
│                    └──────────────┘              │              │
│                                                  ▼              │
│                                    ┌─────────────────────────┐  │
│                                    │  Spring Data JPA        │  │
│                                    │  Repositories           │  │
│                                    │  (Hibernate ORM)        │  │
│                                    └───────────┬─────────────┘  │
└────────────────────────────────────────────────┼────────────────┘
                                                 │  JDBC
                                                 ▼
┌──────────────────────────────────────────────────────────────────┐
│                PostgreSQL 16  (Docker Compose)                   │
│                H2 in-memory              (dev / test profile)    │
└──────────────────────────────────────────────────────────────────┘
```

### Data flow summary

1. The browser sends a JWT in every `Authorization: Bearer` header.
2. `JwtAuthenticationFilter` validates the token and populates the Spring `SecurityContext`.
3. `@PreAuthorize` annotations on controller methods enforce role and project-membership checks before the method body runs.
4. Service methods contain business logic and enforce state-machine transitions.
5. Repositories talk to the database via Hibernate; MapStruct converts JPA entities to response DTOs before the controller serializes them.
6. All exceptions bubble to `GlobalExceptionHandler`, which produces RFC 7807 `ProblemDetail` responses.

---

## 5. Component Design

### 5.1 Frontend (React SPA)

> Detailed design in the frontend repo's `SYSTEM_DESIGN.md`.

| Concern         | Implementation                                 |
|-----------------|------------------------------------------------|
| Routing         | React Router 7 (`ProtectedRoute`, `GuestRoute`) |
| Auth state      | `AuthContext` — persists token in localStorage |
| API layer       | Typed Axios wrappers in `src/api/`             |
| UI              | Unstyled components (CSS modules)              |
| Build           | Vite 8, TypeScript 6                           |

### 5.2 Backend API

The backend is a layered Spring Boot application. Each layer has a single responsibility; dependencies only flow downward.

```
Controller  →  Service  →  Repository  →  Database
     ↑              ↑
  DTOs/MapStruct  Domain Entities
```

| Layer          | Package                          | Responsibility                                       |
|----------------|----------------------------------|------------------------------------------------------|
| **Controller** | `controller/`                    | HTTP contract: parse request, call service, map to DTO, return status code |
| **Service**    | `service/`                       | Business rules, transactional boundaries, authorization enforcement |
| **Repository** | `repository/`                    | Data access via Spring Data JPA; custom JPQL/native queries |
| **Domain**     | `domain/`                        | JPA entities and enums; no business logic            |
| **DTO**        | `dto/`                           | Immutable Java records for API request/response shapes |
| **Security**   | `security/`                      | JWT parsing, `UserDetails`, custom SpEL authorization beans |
| **Config**     | `config/`                        | Security filter chain, CORS, JWT properties, admin bootstrap |
| **Exception**  | `exception/`                     | Custom exception types + `@RestControllerAdvice` handler |
| **Mapper**     | `mapper/`                        | MapStruct compile-time entity↔DTO converters        |
| **Filter**     | `filter/`                        | Correlation ID injection (MDC)                       |

### 5.3 Database

Two schemas used by the same application binary depending on the active Spring profile:

| Profile  | Database   | Purpose                                              |
|----------|------------|------------------------------------------------------|
| `dev`    | H2 in-memory | Fast local iteration; schema created by Hibernate `validate` |
| `prod`   | PostgreSQL 16 | Production; schema managed exclusively by Flyway    |
| `test`   | PostgreSQL 16 (Testcontainers) | Integration tests against a real engine |

---

## 6. Data Model

### 6.1 Entities & Relationships

```
User ──────────────────────────────────────────────────────┐
  │                                                         │
  │ (owner)                                                 │ (member — M:N)
  ▼                                                         ▼
Project ──── project_members (join table) ──────────── User
  │
  │ (1:N)
  ▼
Task ──── assignee_id (FK → User, nullable)
  │
  │ (1:N)
  ▼
Comment ──── author_id (FK → User)
```

### 6.2 Entity Field Reference

#### `users`

| Column          | Type          | Constraints              |
|-----------------|---------------|--------------------------|
| `id`            | UUID          | PK                       |
| `email`         | VARCHAR       | UNIQUE, NOT NULL         |
| `display_name`  | VARCHAR       | NOT NULL                 |
| `password_hash` | VARCHAR       | NOT NULL                 |
| `role`          | VARCHAR (enum)| NOT NULL; ADMIN / MANAGER / MEMBER |
| `created_at`    | TIMESTAMPTZ   | auto-set on insert       |
| `updated_at`    | TIMESTAMPTZ   | auto-set on update       |

#### `projects`

| Column       | Type          | Constraints                          |
|--------------|---------------|--------------------------------------|
| `id`         | UUID          | PK                                   |
| `name`       | VARCHAR       | NOT NULL                             |
| `description`| TEXT          | nullable                             |
| `status`     | VARCHAR (enum)| ACTIVE / ARCHIVED                    |
| `owner_id`   | UUID          | FK → users; NOT NULL                 |
| `created_at` | TIMESTAMPTZ   | auto-set                             |
| `updated_at` | TIMESTAMPTZ   | auto-set                             |

#### `project_members`

| Column       | Type | Constraints                            |
|--------------|------|----------------------------------------|
| `project_id` | UUID | FK → projects ON DELETE CASCADE        |
| `user_id`    | UUID | FK → users                             |
| *(composite PK: project_id + user_id)* | | |

#### `tasks`

| Column        | Type          | Constraints                                          |
|---------------|---------------|------------------------------------------------------|
| `id`          | UUID          | PK                                                   |
| `title`       | VARCHAR       | NOT NULL                                             |
| `description` | TEXT          | nullable                                             |
| `status`      | VARCHAR (enum)| TODO / IN_PROGRESS / IN_REVIEW / DONE / CANCELLED   |
| `priority`    | VARCHAR (enum)| LOW / MEDIUM / HIGH / CRITICAL                      |
| `due_date`    | DATE          | nullable                                             |
| `project_id`  | UUID          | FK → projects ON DELETE CASCADE; INDEX              |
| `assignee_id` | UUID          | FK → users; nullable; INDEX                         |
| `reporter_id` | UUID          | FK → users; NOT NULL                                |
| `created_at`  | TIMESTAMPTZ   | auto-set                                             |
| `updated_at`  | TIMESTAMPTZ   | auto-set                                             |

#### `comments`

| Column      | Type        | Constraints                              |
|-------------|-------------|------------------------------------------|
| `id`        | UUID        | PK                                       |
| `body`      | TEXT        | NOT NULL                                 |
| `task_id`   | UUID        | FK → tasks ON DELETE CASCADE; INDEX     |
| `author_id` | UUID        | FK → users; NOT NULL                    |
| `created_at`| TIMESTAMPTZ | auto-set                                 |

### 6.3 Task Status State Machine

Valid transitions — attempts to jump outside these paths are rejected with `422 Unprocessable Entity`:

```
TODO ──────────────────────────▶ IN_PROGRESS
                                     │
                                     ▼
                                 IN_REVIEW ──▶ DONE
                                     │
                                     ▼
                                 IN_PROGRESS  (rework)

Any state ──────────────────────▶ CANCELLED
```

The machine is encoded in `TaskStatus.canTransitionTo()` — a pure method on the enum, making it testable without a Spring context.

---

## 7. API Design

### 7.1 Conventions

- **Base path:** `/api/v1`
- **Auth:** `Authorization: Bearer <jwt>` on all endpoints except `/auth/**`
- **Pagination:** Query params `page` (0-indexed), `size`, `sort`. Response wrapped in `PageResponse<T>`.
- **Error body:** RFC 7807 `ProblemDetail` (`status`, `detail`, `type` as URI slug).
- **Content-Type:** `application/json` for all requests and responses.
- **ID type:** UUID strings.

### 7.2 Authentication — `/api/v1/auth`

| Method | Path        | Auth | Request Body                    | Response            | Status |
|--------|-------------|------|---------------------------------|---------------------|--------|
| POST   | `/register` | None | `{ email, displayName, password }` | `{ token, user }` | 201    |
| POST   | `/login`    | None | `{ email, password }`          | `{ token, user }`   | 200    |

`AuthResponse` shape:
```json
{
  "token": "<jwt>",
  "user": {
    "id": "<uuid>",
    "email": "alice@example.com",
    "displayName": "Alice",
    "role": "MEMBER"
  }
}
```

### 7.3 Users — `/api/v1/users`

| Method | Path          | Auth                   | Description              |
|--------|---------------|------------------------|--------------------------|
| GET    | `/`           | Any authenticated      | Paginated user list      |
| GET    | `/{id}`       | Any authenticated      | Single user              |
| POST   | `/`           | ADMIN only             | Create user              |
| PATCH  | `/{id}/role`  | ADMIN only             | Update user role         |

### 7.4 Projects — `/api/v1/projects`

| Method | Path                        | Auth                      | Description                   |
|--------|-----------------------------|---------------------------|-------------------------------|
| GET    | `/`                         | Member of any project     | List caller's projects        |
| POST   | `/`                         | Authenticated             | Create project                |
| GET    | `/{id}`                     | Member or ADMIN           | Get project detail            |
| GET    | `/{id}/members`             | Member or ADMIN           | List project members          |
| POST   | `/{id}/members/{userId}`    | Owner or ADMIN            | Add member                    |
| DELETE | `/{id}/members/{userId}`    | Owner or ADMIN            | Remove member                 |
| POST   | `/{id}/archive`             | Owner or ADMIN            | Archive project               |
| PATCH  | `/batch-status`             | ADMIN only                | Bulk status update            |

### 7.5 Tasks — `/api/v1/projects/{projectId}/tasks`

Tasks are always scoped under a parent project — the URL nesting enforces this structurally.

| Method | Path                      | Auth          | Description              |
|--------|---------------------------|---------------|--------------------------|
| GET    | `/`                       | Member/ADMIN  | List tasks (optional `?status=` filter) |
| POST   | `/`                       | Member/ADMIN  | Create task              |
| GET    | `/{taskId}`               | Member/ADMIN  | Get task detail          |
| PATCH  | `/{taskId}/status`        | Member/ADMIN  | Transition task status   |
| PUT    | `/{taskId}/assignee`      | Member/ADMIN  | Assign task              |
| DELETE | `/{taskId}/assignee`      | Member/ADMIN  | Unassign task            |
| DELETE | `/{taskId}`               | Member/ADMIN  | Delete task              |

### 7.6 Comments — `/api/v1/tasks/{taskId}/comments`

| Method | Path            | Auth                  | Description               |
|--------|-----------------|-----------------------|---------------------------|
| GET    | `/`             | Member/ADMIN          | List all comments on task |
| POST   | `/`             | Member/ADMIN          | Add comment               |
| DELETE | `/{commentId}`  | Author or ADMIN       | Delete comment            |

---

## 8. Security Design

### 8.1 Authentication Flow

```
1. Client          POST /api/v1/auth/login  { email, password }
                          │
2. AuthService     DaoAuthenticationProvider.authenticate()
                   → BCrypt password verify
                          │
3. JwtService      generateToken(email)  → signed HMAC-SHA256 JWT
                          │
4. Client          stores token in localStorage
                   attaches on every subsequent request:
                   Authorization: Bearer <token>
                          │
5. JwtAuthFilter   (runs before every request)
                   extracts token → JwtService.extractEmail()
                   → UserDetailsServiceImpl.loadUserByUsername(email)
                   → SecurityContextHolder.setAuthentication(...)
                          │
6. Controller      @PreAuthorize checks role / project membership
```

### 8.2 Authorization Model

Three-layer authorization:

| Layer | Mechanism | Example |
|-------|-----------|---------|
| **Role-based** | `hasRole('ADMIN')` in SpEL | Only ADMIN can create users |
| **Project ownership** | `@projectSecurity.isOwner(id, auth.name)` | Only owner can archive a project |
| **Project membership** | `@projectSecurity.isMember(id, auth.name)` | Only members can create tasks |

`ProjectSecurityService` is a Spring bean registered as `"projectSecurity"` and referenced from SpEL expressions via `@projectSecurity.method(...)`. It queries the database on each call — no caching, which means authorization is always consistent with current membership state.

Comment deletion is the one exception: the service layer enforces author-only delete (with ADMIN bypass), rather than a `@PreAuthorize` expression, because the check requires loading the comment entity first.

### 8.3 JWT Strategy

| Property     | Value                              |
|--------------|------------------------------------|
| Algorithm    | HMAC-SHA256 (HS256)                |
| Claims       | `sub` (email), `iat`, `exp`        |
| Expiry       | 24 hours (`app.jwt.expiry-ms`)     |
| Secret       | From `JWT_SECRET` env var (prod); dev default in properties |
| Storage      | Stateless — no server-side session; no token revocation |

**Known limitation:** there is no token revocation mechanism. A stolen token is valid until expiry. Mitigation approaches (Redis token blacklist, short expiry + refresh tokens) are listed in §15.

### 8.4 CORS

CORS is configured in `SecurityConfig` via Spring's `CorsConfigurationSource`. Allowed origins are driven by `app.cors.allowed-origins` (dev default: `http://localhost:5173`). In production this should be the exact frontend origin — wildcards are not acceptable when credentials (JWT) are involved.

### 8.5 Other Security Hardening

- `SessionCreationPolicy.STATELESS` — no `JSESSIONID` cookie is ever created.
- CSRF disabled — CSRF attacks require a browser cookie; JWT in `Authorization` header is not auto-sent by the browser, so CSRF is not a threat.
- `HttpStatusEntryPoint(UNAUTHORIZED)` — unauthenticated requests receive 401, not a redirect to a login page.
- `spring.jpa.open-in-view=false` — prevents lazy Hibernate sessions from leaking across the HTTP layer.

---

## 9. Storage Design

### 9.1 Migration Strategy

Schema is managed exclusively by **Flyway**. Hibernate's `ddl-auto` is set to `validate` in production — Hibernate will fail fast on startup if the schema doesn't match the entity model, but will never alter it.

| Environment | Schema source         | Flyway enabled |
|-------------|----------------------|----------------|
| dev         | Hibernate auto-create | No             |
| test        | Flyway (Testcontainers) | Yes          |
| prod        | Flyway (PostgreSQL)   | Yes            |

Migration naming convention: `V{N}__{description}.sql`

| Migration | Tables created                         |
|-----------|----------------------------------------|
| V1        | `users`                                |
| V2        | `projects`, `project_members`          |
| V3        | `tasks`                                |
| V4        | `comments`                             |

### 9.2 Indexing Strategy

| Index                        | Rationale                                                   |
|------------------------------|-------------------------------------------------------------|
| `tasks.project_id`           | Every task query is scoped to a project                     |
| `tasks.assignee_id`          | "Tasks assigned to me" queries                              |
| `tasks.status`               | Filtered task list by status                                |
| `comments.task_id`           | Comments are always fetched for a specific task             |
| `users.email` (UNIQUE)       | Login lookup + uniqueness enforcement                       |

### 9.3 UUID Primary Keys

All PKs are `UUID` generated by `GenerationType.UUID` (Java UUID strategy). Trade-off vs. auto-increment:

- **Pro:** IDs are safe to expose in URLs without enumeration risk; work across distributed inserts.
- **Con:** UUIDs are 16 bytes vs. 4–8 bytes for integers; random UUIDs cause B-tree page splits (can use UUID v7 for sequential UUIDs if this becomes a bottleneck).

### 9.4 JDBC Batch Configuration

```properties
spring.jpa.properties.hibernate.jdbc.batch_size=500
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
```

Hibernate groups inserts/updates of the same type into a single JDBC batch call. `order_inserts/updates=true` ensures statements are sorted by type so batching is effective. This matters for the batch project-status update endpoint and any future bulk operations.

---

## 10. Observability

### 10.1 Correlation IDs

`CorrelationFilter` runs at `Ordered.HIGHEST_PRECEDENCE`. On every request it:

1. Reads `X-Correlation-ID` header (or generates a UUID if absent).
2. Stores it in SLF4J MDC under key `correlationId`.
3. Echoes it back in the response as `X-Correlation-ID`.

Every log line emitted during that request will include the correlation ID. When a client reports an error, they can provide this header value to pinpoint the exact request trace in logs.

### 10.2 Metrics

Powered by **Micrometer** (Spring Boot Actuator auto-configuration).

| Metric                  | Type    | Tags             | Where emitted  |
|-------------------------|---------|------------------|----------------|
| `auth.registrations`    | Counter | —                | `AuthService`  |
| `tasks.created`         | Counter | `priority=<val>` | `TaskService`  |

Micrometer is backend-agnostic. Exporting to Prometheus, Datadog, or CloudWatch requires only a dependency change — no application code changes.

### 10.3 Health & Actuator

| Endpoint             | Auth required | Purpose                        |
|----------------------|---------------|--------------------------------|
| `/actuator/health`   | No            | Liveness / readiness probe     |
| `/actuator/info`     | No            | Build info, version            |
| `/actuator/metrics`  | No            | All registered Micrometer meters |

Exposing `/actuator/metrics` without auth is acceptable in a private network. In a public deployment, consider securing it behind a management port or a sidecar.

---

## 11. Error Handling

### 11.1 Error Body Format (RFC 7807)

All errors return a `ProblemDetail` JSON body:

```json
{
  "type": "https://taskflow.local/errors/resource-not-found",
  "title": "Not Found",
  "status": 404,
  "detail": "Task with id 'abc-123' not found"
}
```

Using a standard format means clients can parse errors generically rather than handling each endpoint's unique error shape.

### 11.2 Exception Taxonomy

| Exception class          | HTTP Status | Meaning                                       |
|--------------------------|-------------|-----------------------------------------------|
| `ResourceNotFoundException` | 404      | Entity looked up by ID doesn't exist          |
| `BusinessRuleException`  | 422         | Valid request but violates a business rule (e.g., invalid status transition) |
| `ConflictException`      | 409         | Duplicate resource (e.g., email already registered) |
| `ForbiddenException`     | 403         | Authenticated but not authorized              |
| `MethodArgumentNotValidException` | 400 | Bean Validation (`@Valid`) failure on request body |
| `AuthenticationException`| 401         | Missing or invalid JWT                        |

Throwing a typed exception from a service is preferable to manually returning `ResponseEntity<>` with a status code, because it keeps service methods free of HTTP concerns.

---

## 12. Performance Considerations

### 12.1 N+1 Query Prevention

Hibernate's default lazy loading causes N+1 queries: fetching a list of 20 tasks and accessing `task.getAssignee()` issues 20 separate `SELECT` statements. TaskFlow prevents this with `@EntityGraph` annotations on repository methods, which generate a single `JOIN FETCH` query.

Example: `TaskRepository.findByProjectId(id, pageable)` eagerly loads `assignee` and `reporter` in one query.

### 12.2 Batch Project Status Update

`PATCH /api/v1/projects/batch-status` processes updates in chunks (default: 100 records per chunk) using a `@Modifying` JPQL `UPDATE` query. This avoids loading thousands of entities into memory just to set a single field.

Chunking also limits transaction size: a single transaction updating 100,000 rows holds locks for the entire duration. Smaller transactions reduce lock contention at the cost of partial failure visibility.

### 12.3 Read-Only Transactions

All service methods default to `@Transactional(readOnly = true)`. This tells Hibernate to skip dirty-checking on managed entities at flush time — a measurable win for read-heavy workloads. Mutating methods override to `@Transactional` (read-write).

### 12.4 `open-in-view=false`

Spring Boot defaults `open-in-view` to `true`, which keeps the Hibernate session open for the entire HTTP request. This enables lazy loading in the view layer but hides performance problems and causes unexpected database calls outside the service layer. TaskFlow disables it, which forces all data fetching to happen explicitly within `@Transactional` service methods.

---

## 13. Deployment

### 13.1 Environment Overview

| Environment | Backend          | Database           | Frontend           |
|-------------|------------------|--------------------|--------------------|
| Local dev   | `./mvnw spring-boot:run` (port 8080) | H2 in-memory | `npm run dev` (port 5173, Vite) |
| Docker prod | `docker-compose up` | PostgreSQL 16 container | Static files served by nginx or CDN |

### 13.2 Docker Compose (Production)

`docker-compose.yml` defines two services:

- **`db`**: `postgres:16-alpine`, persists data to a named volume.
- **`api`**: built from `Dockerfile`, depends on `db`, exposes port 8080.

The `prod` Spring profile is activated via the `SPRING_PROFILES_ACTIVE=prod` environment variable. Flyway runs on startup and applies pending migrations before the application accepts traffic.

### 13.3 Required Environment Variables

| Variable              | Required in | Purpose                           |
|-----------------------|-------------|-----------------------------------|
| `JWT_SECRET`          | prod        | HMAC-SHA256 signing key (min 256 bits, base64-encoded) |
| `DB_URL`              | prod        | JDBC connection string            |
| `DB_USERNAME`         | prod        | Database user                     |
| `DB_PASSWORD`         | prod        | Database password                 |
| `CORS_ALLOWED_ORIGINS`| prod        | Frontend origin (e.g., `https://taskflow.example.com`) |

`.env.example` in the project root documents all variables. Never commit `.env` to version control.

### 13.4 Admin Bootstrap

On startup, `AdminBootstrapRunner` checks whether an ADMIN user exists. If not, it creates `admin@taskflow.local` with a randomly generated password logged at WARN level. This ensures the application is accessible after a fresh deployment without manual database seeding.

---

## 14. Trade-offs & Alternatives Considered

### 14.1 JWT vs. Session Cookies

| | JWT (chosen) | Server-side sessions |
|-|---|---|
| **State** | Stateless — no server memory per user | Stateful — session store required |
| **Scalability** | Any instance validates any token | All instances must share session store (Redis) |
| **Revocation** | Hard — must wait for expiry | Easy — delete from store |
| **Decision** | Chosen because stateless horizontal scaling is simpler at this stage. Revocation support can be added with a Redis blacklist if needed. | |

### 14.2 H2 vs. PostgreSQL for Development

H2's in-memory mode makes local dev startup instant (no Docker dependency). The risk is H2/PostgreSQL behavioral divergence hiding bugs. Mitigation: integration tests always run against a real PostgreSQL container (Testcontainers), so divergence is caught before merge.

### 14.3 Flyway vs. Hibernate auto-DDL

`spring.jpa.hibernate.ddl-auto=update` is convenient but dangerous in production: it can silently add columns but will never drop them, and its behavior on type changes is undefined. Flyway gives explicit, version-controlled, peer-reviewed migrations. The cost is an extra SQL file per schema change — worth it.

### 14.4 MapStruct vs. Manual Mapping vs. ModelMapper

| | MapStruct (chosen) | Manual | ModelMapper |
|-|---|---|---|
| **Performance** | Compile-time generated code, zero reflection | Same | Reflection at runtime |
| **Type safety** | Compile errors on mismatched fields | Compile errors | Runtime errors |
| **Boilerplate** | Minimal (interface + annotations) | Verbose | Minimal |
| **Decision** | MapStruct gives the safety of manual mapping with near-zero boilerplate. ModelMapper is convenient but runtime reflection masks mapping errors until they surface in production. | | |

### 14.5 UUID vs. Auto-increment PKs

UUIDs allow ID generation client-side or in distributed systems without a central sequence. They are also safe to expose in URLs (no sequential enumeration). Downside: 16-byte random values create B-tree fragmentation under high insert rates. At TaskFlow's scale this is irrelevant; for future mitigation see UUID v7 (time-ordered).

### 14.6 `@PreAuthorize` SpEL vs. Service-layer Checks

Most authorization is enforced at the controller boundary via `@PreAuthorize`. This fails fast before any service code runs. The exception is comment deletion, where the business rule requires loading the entity first (to compare `comment.author == caller`). This check lives in the service layer to keep the controller free of entity-loading logic.

---

## 15. Scalability Limits & Future Work

### Current Bottlenecks (in order of likely impact)

| Bottleneck | Limit | Mitigation path |
|------------|-------|-----------------|
| **Single PostgreSQL instance** | Vertical limit; no read replicas | Add read replicas; route read-only queries via secondary datasource |
| **No token revocation** | Stolen JWTs valid for 24 hours | Short-lived access tokens (15 min) + Redis-backed refresh tokens |
| **No rate limiting** | Any client can hammer the API | API gateway (Kong, AWS API Gateway) or Spring's `Bucket4j` library |
| **No caching** | Every project-membership check hits the DB | Cache `ProjectSecurityService` results in Caffeine/Redis with invalidation on membership change |
| **In-process batch processing** | Batch updates block the request thread | Offload to a job queue (Spring Batch, AWS SQS) for very large datasets |
| **No connection pooling config** | HikariCP uses defaults | Tune pool size to `(core_count * 2) + effective_spindle_count` |

### Planned Features

- **Module 7 — Observability:** Structured JSON logging, Prometheus scrape endpoint, distributed tracing (Micrometer Tracing + Zipkin/Jaeger).
- **Module 8 — Async Events:** Spring Application Events or messaging (RabbitMQ/Kafka) for post-mutation side effects (email notifications, audit log).
- **Module 9 — Refresh Tokens:** Short-lived access tokens + revocable refresh tokens stored in the database.
- **Module 10 — Rate Limiting:** Per-user request throttling using Bucket4j.

---

## 16. Open Questions

| # | Question | Owner | Status |
|---|----------|-------|--------|
| 1 | Should archived projects be excluded from member queries by default, or filtered client-side? | Backend | Open |
| 2 | Task `dueDate` is stored as `DATE` (no timezone). If users are in multiple timezones, should this be `TIMESTAMPTZ`? | Backend | Open |
| 3 | Comment editing: should comments be immutable after creation, or allow edits within a time window (e.g., 5 minutes)? | Product | Open |
| 4 | Should `MANAGER` role have a distinct permission set from `MEMBER`, or is the current binary (member / admin) sufficient? | Product | Open |
| 5 | Pagination on comments: currently a flat list. Will tasks with thousands of comments require cursor-based pagination? | Backend | Open |

---

## 17. Glossary

| Term | Definition |
|------|------------|
| **JWT** | JSON Web Token — a signed, self-contained token used for stateless authentication |
| **JJWT** | Java JWT library (io.jsonwebtoken) used for signing and parsing JWTs |
| **Flyway** | Database migration tool that manages schema version history |
| **MapStruct** | Compile-time annotation processor that generates type-safe bean mappers |
| **Micrometer** | Vendor-neutral metrics instrumentation library for the JVM |
| **MDC** | Mapped Diagnostic Context — SLF4J thread-local key-value store included in log output |
| **ProblemDetail** | RFC 7807 standard JSON error body format |
| **SpEL** | Spring Expression Language — used in `@PreAuthorize` annotations |
| **Testcontainers** | Java library that spins up real Docker containers (e.g., PostgreSQL) during tests |
| **EntityGraph** | JPA hint that forces eager loading of specified associations to avoid N+1 queries |
| **N+1 problem** | When loading N entities and accessing a lazy association results in N additional queries |
| **CORS** | Cross-Origin Resource Sharing — browser security policy; must be explicitly configured for the SPA origin |
| **BCrypt** | Adaptive password hashing algorithm with a configurable cost factor |
| **Correlation ID** | A UUID attached to every request and echoed in every log line, enabling full request trace reconstruction |
