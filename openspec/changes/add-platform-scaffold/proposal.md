# Proposal

## Why

The repository has a product spec but no code. Every later change (auth, todo items, UI, admin) needs the same foundation: a workspace layout, runnable services, cross-cutting API behavior (errors, logging, CORS) and a CI gate. Building it once, first, keeps those concerns out of the feature changes.

## What Changes

- Create the workspace layout: `backend/` (Spring Boot 3, Java 21, Maven), `frontend/` (React + Vite + TypeScript), `.github/workflows/`, root `docker-compose.yml` and `.env.example`.
- Add multi-stage Dockerfiles for backend and frontend, each with a `dev` and a `prod` target.
- Add a Docker Compose stack linking frontend, backend, PostgreSQL, Redis and a dev mail catcher (Mailpit).
- Add a GitHub Actions pipeline that lints, tests and builds Docker images.
- Backend cross-cutting behavior: layered package structure, RFC 7807 problem-details error handling, JSON structured logging, CORS restricted to one configured frontend origin, Flyway migrations wired up, Redis cache manager wired up, `/api/v1` base path and a health endpoint.
- Frontend foundation: folder structure (components, features, hooks, routing, store), Redux store, base RTK Query API slice, Tailwind CSS v4 via the Vite plugin, Vitest + React Testing Library + MSW test harness.
- Test infrastructure: JUnit 5, Mockito, AssertJ, Testcontainers (PostgreSQL), JaCoCo with an 80% line-coverage gate; Vitest coverage with an 80% threshold.

No product features are included. Auth, todo items, UI screens and the admin view are separate changes.

## Capabilities

### New Capabilities
- `api-platform`: Cross-cutting behavior every API endpoint shares: base path, RFC 7807 error format, structured JSON logs, CORS policy, health check.
- `dev-environment`: One-command local stack and the CI gate: Compose services, required configuration, and what the pipeline enforces.

### Modified Capabilities
<!-- None: no specs exist yet. -->

## Impact

- New top-level directories and files only; no existing code is touched.
- Introduces the dependency set for the whole project (Spring Boot starters, Flyway, Spring Data Redis, Spring Mail, jjwt, React, Redux Toolkit, React Router v7, Tailwind v4, Vitest, MSW).
- Defines the environment variables later changes rely on: `FRONTEND_ORIGIN`, `JWT_SECRET`, database and Redis settings, SMTP settings.
