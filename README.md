# TODO app

Multi-user TODO web app: React + Vite frontend, Spring Boot backend, PostgreSQL, Redis.
Product and tech requirements live in `project_description.md`; planned work lives in `openspec/`.

## Prerequisites

- Docker with Compose v2 (needed for the stack **and** for backend tests, which use Testcontainers)
- Optional, for running outside Docker: Java 21, Node.js 22+ (Maven is provided by `backend/mvnw`)

## Run the whole stack

```bash
cp .env.example .env
# edit .env and set JWT_SECRET (e.g. `openssl rand -base64 48`)
docker compose up --build      # start (add -d to detach)
docker compose down            # stop; add -v to also delete the database volume
```

`docker compose up` also reads `docker-compose.override.yml`, which bind-mounts source for the `dev` targets.
Set `BUILD_TARGET=prod` in `.env` for production-like images (nginx-served frontend, JRE-only non-root backend).

| Service  | URL                                   |
| -------- | ------------------------------------- |
| Frontend | http://localhost:5173                 |
| Backend  | http://localhost:8080/api/v1/health   |
| Mailpit  | http://localhost:8025 (caught emails) |

If `JWT_SECRET` is empty the backend refuses to start and logs that `JWT_SECRET` is required.

## Run tests

```bash
# backend (Docker must be running for the Testcontainers tests)
cd backend && ./mvnw verify          # tests + 80% line-coverage gate (JaCoCo)

# frontend
cd frontend
npm ci
npm run lint
npm test                             # Vitest + React Testing Library + MSW
npm run test:coverage                # fails below 80% line coverage
npm run build
```

## Continuous integration

`.github/workflows/ci.yml` runs on pull requests and pushes to `main`: `backend` and `frontend` jobs (lint, tests, coverage),
then a `docker` job that builds the `prod` images (only if both pass; nothing is pushed).

## Layout

```
backend/    Spring Boot 3 (Java 21, Maven): controller / service / repository / entity / config / exception
frontend/   React + Vite + TypeScript: app, components, features, hooks, routing, store
docker-compose.yml, docker-compose.override.yml, .env.example
```
