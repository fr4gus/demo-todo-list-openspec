# Design

## Context

Greenfield repository: only `project_description.md` and OpenSpec files exist. The tech stack is fixed by that document (React/Vite/TS, RTK Query, React Router v7, Tailwind v4, Spring Boot 3 on Java 21, PostgreSQL, Redis, Compose, GitHub Actions). See proposal.md for motivation.

## Goals / Non-Goals

**Goals:**
- A layout and conventions the four feature changes can slot into without rework.
- Cross-cutting concerns (errors, logging, CORS, caching, migrations) configured once.
- `docker compose up` yields a working stack; CI mirrors what developers run locally.

**Non-Goals:**
- Any product feature, entity or screen.
- Production deployment (registry publishing, hosting, TLS, secrets management).
- Observability beyond JSON logs and a health endpoint.

## Decisions

**Layout.** Monorepo:
```
backend/    pom.xml, Dockerfile, src/main/java/.../{controller,service,repository,entity,config,exception}
frontend/   package.json, Dockerfile, vite.config.ts, src/{app,components,features,hooks,routing,store}
.github/workflows/ci.yml
docker-compose.yml, .env.example
```
Alternative: separate repos. Rejected; one spec-driven workflow and one compose file are simpler together.

**Maven, Flyway.** `pom.xml` is named in the spec, so Maven. Flyway over Liquibase: plain SQL migrations are easier to review and the schema is small. Flyway runs at startup; `spring.jpa.hibernate.ddl-auto=validate` so the schema is only changed by migrations.

**Audit columns.** A `@MappedSuperclass` base entity with `created_at` / `updated_at` using JPA auditing. Migrations also set `DEFAULT now()` so manual inserts stay valid.

**Errors.** One `@RestControllerAdvice` returning Spring's `ProblemDetail`, with a `code` property for application exceptions (`ApiException` hierarchy). Spring Security's entry point and access-denied handler write the same format so 401/403 match.

**Logging.** Logback with a JSON encoder (logstash-logback-encoder) as the default profile, a plain-text console pattern only under a `local` profile. A servlet filter puts a correlation id (incoming `X-Request-Id` or generated) into MDC and the response header. Request bodies and `Authorization` are never logged.

**CORS.** Single `CorsConfigurationSource` bean built from `FRONTEND_ORIGIN`; no wildcard, credentials not enabled because the token is sent in a header, not a cookie.

**Caching.** `@EnableCaching` with `RedisCacheManager`, JSON value serialization, a default TTL of 10 minutes and per-cache names declared in one config class. Feature changes add their own caches.

**Config and secrets.** `application.yml` reads everything from environment variables with safe local defaults only for non-secrets (ports, hostnames). `JWT_SECRET` has no default, so a missing value fails startup via `@ConfigurationProperties` validation.

**Compose.** Services: `frontend`, `backend`, `postgres`, `redis`, `mailpit`. Healthchecks on postgres and redis; backend `depends_on: condition: service_healthy`. Dockerfile `dev` and `prod` targets are selected through a `BUILD_TARGET` variable defaulting to `dev`.

**Frontend base.** A `baseApi` created with `createApi` and a `fetchBaseQuery` whose `prepareHeaders` adds `Authorization: Bearer <token>` from the auth slice (the slice itself is added by the auth change; the scaffold ships the slice-agnostic wiring and a placeholder reading from the store). Folders: `app/` (store, providers), `components/` (shared UI), `features/<name>/`, `hooks/`, `routing/` (router and `ProtectedRoute` shell).

**CI.** One workflow, three jobs: `backend` (Temurin 21, `mvn verify` with Testcontainers on the runner's Docker), `frontend` (Node LTS, `npm ci`, lint, `vitest run --coverage`, build), `docker` (needs both; builds `prod` targets, no push).

**Coverage gate.** JaCoCo `check` rule at 80% line coverage on the backend; Vitest `coverage.thresholds.lines = 80`. The gate starts when the first features land; the scaffold's own code is excluded from the threshold only where trivially untestable (main class, config records).

## Risks / Trade-offs

- [Testcontainers needs Docker on developer machines and CI] → GitHub-hosted runners provide it; document the requirement.
- [80% gate can block the very first PRs, when little code exists] → Scope coverage to packages that contain logic; include sample tests required by the spec.
- [Placeholder auth wiring in the scaffold could drift from the auth change] → Keep it to one selector function that the auth change replaces.
- [Flyway plus `ddl-auto=validate` makes entity mistakes fail at startup] → Accepted; failing early is the point.
