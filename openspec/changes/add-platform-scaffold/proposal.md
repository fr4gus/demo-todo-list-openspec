# Proposal

## Why

The repository holds a product specification and no code. Every later change — accounts, todo items, the list screen, the admin view — needs the same foundation: a workspace that builds, a local stack that runs, one error format, and a CI gate that keeps coverage honest. Building that once, first, keeps cross-cutting concerns out of the feature changes.

## What Changes

- Create the workspace layout: `backend/` (Spring Boot 3, Java 21, Maven), `frontend/` (React + Vite + TypeScript), `.github/workflows/`, and a root `docker-compose.yml` with a `.env.example`.
- Add multi-stage Dockerfiles for the backend and the frontend, each with a `dev` and a `prod` target.
- Add a Docker Compose stack linking frontend, backend, PostgreSQL, Redis and a mail catcher.
- Add a GitHub Actions pipeline that lints, tests and builds the container images, with an 80% line-coverage gate on both sides.
- Backend foundation: layered package structure, `/api/v1` base path, RFC 7807 problem-details errors carrying a machine-readable `code`, JSON structured logging, CORS restricted to a single configured frontend origin, Flyway migrations, a Redis cache manager, and a health endpoint.
- Frontend foundation: folder structure separating components, features, hooks, routing and store, the Redux store, a base RTK Query API slice with JWT header injection, React Router v7, Tailwind CSS v4, and a Vitest + React Testing Library + MSW harness.
- Test foundation: JUnit 5, Mockito and AssertJ; Testcontainers PostgreSQL for repository tests; JaCoCo and Vitest coverage configured as a hard 80% gate.

No product feature is included. Accounts, todo items, the list screen and the admin view are separate changes.

## Capabilities

### New Capabilities

- `api-platform`: The behavior every API endpoint shares — the base path, the problem-details error contract, structured logging, the CORS policy, and the health check.
- `dev-environment`: The one-command local stack and the CI gate — which services the stack provides, what configuration it requires, and what the pipeline enforces.

### Modified Capabilities

<!-- None: no specs exist yet. -->

## Impact

- New top-level directories and files only; no existing code is touched.
- Establishes the dependency set for the whole project: Spring Boot starters, Flyway, Spring Data Redis, Spring Mail, jjwt, React, Redux Toolkit, React Router v7, Tailwind CSS v4, Vitest, MSW.
- Defines the environment variables later changes rely on: `FRONTEND_ORIGIN`, `JWT_SECRET`, database and Redis settings, SMTP settings.
