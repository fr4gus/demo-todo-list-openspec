# TODO app

Multi-user TODO web app: React + Vite frontend, Spring Boot backend, PostgreSQL, Redis.
Product and tech requirements live in `project_description.md`; planned work lives in `openspec/`.

## Prerequisites

Versions below are what the project is developed and tested with. Newer minor versions should work.

| Tool                   | Version                 | Needed for                                                        |
| ---------------------- | ----------------------- | ----------------------------------------------------------------- |
| Docker Engine / Desktop | 25+ (tested with 29.x) | Running the stack **and** backend tests (Testcontainers)          |
| Docker Compose         | v2 plugin (`docker compose`) | Running the stack                                            |
| Git                    | any recent              | Cloning                                                           |
| JDK                    | **21** (Temurin or OpenJDK) | Backend outside Docker, `./mvnw verify`                       |
| Node.js                | **22 LTS**              | Frontend outside Docker (`npm ci`, tests, lint)                   |
| npm                    | bundled with Node       | Frontend                                                          |
| Maven                  | *not required*          | Provided by the wrapper `backend/mvnw` (Maven 3.9.9)              |

Only Docker is needed to **run** the app. Java and Node are only needed if you run tests or develop outside containers.
Check your setup with `docker --version`, `docker compose version`, `java -version` and `node -v`.

Free local ports: `5173` (frontend), `8080` (backend), `8025` (Mailpit). Change them in `.env` if they clash.

> **Docker Desktop on Linux:** Testcontainers looks for `/var/run/docker.sock` first. If backend tests are skipped or
> cannot find Docker, run them with `DOCKER_HOST=unix://$HOME/.docker/desktop/docker.sock ./mvnw verify`.
> The project pins Testcontainers 1.21.4, which is required to talk to Docker Engine 29 (older versions are rejected
> by it).

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

### Check that it works

Nothing product-facing exists yet (sign-in, TODO list and admin arrive in later changes), so a smoke check is:

1. `curl http://localhost:8080/api/v1/health` returns `{"status":"UP"}`.
2. Open http://localhost:5173. You are redirected to `/login` and see a "Sign in" heading (the TODO page is behind login).
3. `curl http://localhost:5173/api/v1/health` returns the same `UP`, proving the frontend proxy reaches the backend.
4. `curl -i http://localhost:8080/api/v1/anything` returns `401` with an `application/problem+json` body.
5. Open http://localhost:8025 to see Mailpit (empty until password recovery exists).

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
