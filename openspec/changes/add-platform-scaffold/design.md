# Design

## Context

The repository contains a product specification (`project_description.md`) and the OpenSpec workflow scaffolding. No application code, build files or tests exist. There are no build, lint or test commands to run until the two projects are scaffolded.

The specification prescribes the stack in detail: Spring Boot 3 on Java 21 with PostgreSQL, Redis, stateless JWT and RBAC; React with Vite and TypeScript, Redux Toolkit with RTK Query, React Router v7, Tailwind CSS v4; Docker Compose; a CI pipeline; Vitest/RTL/MSW, JUnit 5/Mockito/AssertJ, Testcontainers with real PostgreSQL, and an 80% coverage baseline. The constraints this design must honor are the RFC 7807 error format, JSON structured logging, BCrypt password storage, a CORS policy restricted to one frontend origin, `@Cacheable`/`@CacheEvict` Redis caching, and audit fields on tables.

Two ambiguities in the specification are resolved here rather than deferred: the migration tool is "Liquibase/Flyway" (an either/or), and the CI provider is "GitHub Actions / GitLab CI" (an either/or). The repository's remote is on GitHub, so GitHub Actions is the concrete answer. The toolchain on this machine has Java 21, Node 22 and Docker, but no system Maven.

## Goals / Non-Goals

**Goals:**

- A workspace where each side builds, tests and runs on its own, and where the whole system runs from one command.
- An error contract that every later change can rely on without redefining error handling.
- A CI gate that lints, tests, enforces coverage and builds images, so that no later change can merge with a red pipeline.
- Test harnesses that later changes can write against directly rather than each inventing their own.

**Non-Goals:**

- Any product behavior: no accounts, no todos, no screens. Those are separate changes.
- Choosing a deployment platform, orchestrator or cloud provider.
- Multi-environment configuration beyond local development.

## Decisions

**Maven with a checked-in wrapper.** The specification names `pom.xml`, and the Maven wrapper (`mvnw`, `mvnw.cmd`, `.mvn/wrapper/`) is committed so that neither CI nor a developer needs a system Maven. Gradle was rejected: the specification names a `pom.xml`, and nothing in it needs Gradle's build cache or Kotlin DSL.

**Flyway over Liquibase.** The specification allows either. Flyway's migration model is a single ordered directory of versioned SQL files, which is easier to read in review and needs no separate changelog XML. Liquibase's main advantage is automated rollback, which this project does not require — Flyway's `undo` is a Community-edition limitation, but the project has no production data during development. Decision: Flyway, with SQL migrations under `src/main/resources/db/migration`.

**RFC 7807 via Spring's `ProblemDetail`, extended with a `code` property.** Spring Framework 6 ships `org.springframework.http.ProblemDetail`, so the `@RestControllerAdvice` returns `ProblemDetail` rather than hand-building JSON. The `code` member is the project's addition: `ProblemDetail` carries `type`, `title`, `status`, `detail` and `instance`, but nothing a client can branch on programmatically beyond the status. Setting `problem.setProperty("code", "EMAIL_NOT_FOUND")` gives every later change a single stable discriminator, which matters because several failures share a status (the specification mandates both "invalid password" and "Email doesn't exist", both HTTP 401). Every error in the system carries a `code`; the `code` catalogue is documented in the API spec so it can be relied on.

**Structured logging via Spring Boot's built-in support, with a documented fallback.** Spring Boot 3.4 introduced structured logging, set by `logging.structured.format.console=ecs`. Using it avoids adding `logstash-logback-encoder` and the version pinning that comes with it. If the pinned Spring Boot version turns out to predate 3.4, the fallback is `logstash-logback-encoder` with an ECS layout — a two-file change confined to `logback-spring.xml`. The request log record is emitted by a servlet filter, so exactly one record per request is guaranteed and the duration is measured in one place.

**CORS via a single `CorsConfigurationSource` bean.** The allowed origin comes from `FRONTEND_ORIGIN`; `allowCredentials` is `false` because the API authenticates with an `Authorization` header rather than a cookie, which is both simpler and immune to CSRF. Allowed methods are limited to the verbs the API uses, and `Authorization` and `Content-Type` are the only allowed request headers. A permissive wildcard was rejected — the specification requires strictness to one origin.

**Redis cache manager with per-cache TTL and JSON serialization.** `RedisCacheManager` is configured with `GenericJackson2JsonRedisSerializer` rather than JDK serialization: JDK-serialized blobs are version-brittle across Spring releases and are a known deserialization risk. Each cache name gets its own TTL, because Spring's default is infinite and an infinite TTL turns a missed eviction into a permanently stale value. The `todos` cache is created here with a 60-second TTL; the `auth` cache for token versions is added by the auth change. Caching is disabled in the test profile so that unit and service tests need no Redis.

**Testcontainers for PostgreSQL, and no H2.** The specification forbids H2 so that dialect behavior (enums, `timestamptz`, `COUNT ... FILTER`, partial indexes) is exercised against the real engine. Repository tests get a shared PostgreSQL container; because the containers start once and are reused across the suite, the cost is one container per build rather than one per test class.

**Coverage exclusions are configured up front, not discovered on the first red build.** The 80% gate is a hard failure in CI, as the specification requires. The common way this goal is missed is incidental code: configuration classes, DTOs, entities, records, the application entry point, and frontend files such as `main.tsx`, type declarations and test setup. JaCoCo exclusions and the Vitest coverage `exclude` list cover exactly those categories. This is the single most likely cause of a spurious CI failure in a from-scratch project, so it is settled in the scaffold rather than during a later change.

**Frontend structure, store and API slice.** `src/` separates `app/` (providers), `components/` (shared presentational), `features/` (one folder per domain, each owning its slice, components and hooks), `hooks/`, `routing/`, `store/` and `lib/` (configure-store, typed hooks). RTK Query's `fetchBaseQuery` gains a `prepareHeaders` callback that reads the bearer token from the auth slice and attaches it. The auth slice itself arrives with the auth change; the scaffold ships the store, the base slice with a placeholder token selector, and the router with a single placeholder route. The `ProtectedRoute` wrapper arrives with the UI change.

**MSW as the network mock.** Handlers live in `src/mocks/handlers/` with a `server.ts` that the Vitest setup file starts per test file. A default handler set is exported so that each test can override only the endpoints it cares about, which keeps component tests focused on the endpoint under test rather than on the whole API surface.

**Docker images with `dev` and `prod` targets.** Both Dockerfiles are multi-stage. The `prod` target contains only the built artifact on a slim runtime base. The `dev` target runs the app with the source mounted for hot reload. Compose uses the `dev` targets; the CI image-build job builds the `prod` targets, so the image that is built and the image that runs locally are not the same image — this is a known and accepted gap, noted here so it is not mistaken for coverage.

**CI as three jobs.** `backend` runs `./mvnw -B verify`, which compiles, runs the tests and applies the JaCoCo check. `frontend` runs lint, then tests with coverage, then a production build. `docker` builds both `prod` images. The frontend job needs Node and the backend job needs Java, so the jobs are separate rather than a matrix, and the docker job depends on both. A `testcontainers` requirement is satisfied by the runner having Docker available, which GitHub-hosted Linux runners do.

## Risks / Trade-offs

- [The 80% coverage gate blocks the first CI run on incidental code] → Exclusions are configured in this change for both projects, and every later change ships its own tests in the same group as the work they cover, so a red pipeline always means a real gap.
- [The `todos` cache can serve a stale list] → The 60-second TTL bounds staleness, and the cache-aside race where a concurrent read repopulates a just-evicted key is documented rather than solved. The client-side cache is the one users actually notice, and RTK Query invalidation covers the common case.
- [Structured logging adds volume and cost] → Log level is configurable per environment, and the request record contains no request or response body.
- [Two images exist: the locally-run dev target and the CI-built prod target] → Accepted. The `prod` target is what CI proves builds; the `dev` target is a convenience. A follow-up could add a compose profile that runs the `prod` targets.
- [Testcontainers slows the build] → Containers are reused across the suite and the suite runs once per CI job rather than per test class.
- [Tailwind CSS v4 setup differs from v3 conventions] → The v4 Vite plugin is used as the specification requires; the CSS-first configuration is adopted rather than a v3 `tailwind.config.js`.
