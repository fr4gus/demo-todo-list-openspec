# Spec Delta

## Purpose

Defines how a developer runs the whole system locally with one command and what the CI pipeline enforces before changes merge.

## ADDED Requirements

### Requirement: One-command local stack
The repository SHALL provide a Docker Compose file that starts the frontend, backend, PostgreSQL, Redis and a dev mail catcher together, with the backend waiting for healthy databases.

#### Scenario: Fresh start
- **WHEN** a developer copies `.env.example` to `.env`, fills in the required secrets and runs `docker compose up --build`
- **THEN** all services start, the backend reports healthy at `/api/v1/health`, and the frontend is reachable in the browser

#### Scenario: Missing required secret
- **WHEN** a required secret such as `JWT_SECRET` is not set
- **THEN** the backend refuses to start and logs which variable is missing

### Requirement: Development and production image targets
Each service Dockerfile SHALL be multi-stage with a `dev` target (fast feedback, source mounted) and a `prod` target (minimal runtime image, no build tooling).

#### Scenario: Production build
- **WHEN** the `prod` target of each Dockerfile is built
- **THEN** the backend image runs as a non-root user with only the JRE and the application jar, and the frontend image serves the static build

### Requirement: No secrets in the repository
Committed files SHALL contain no real credentials; secrets come from environment variables and `.env.example` documents each variable with an empty or obviously fake value.

#### Scenario: Example file
- **WHEN** `.env.example` is reviewed
- **THEN** it lists every required variable and no real secret value

### Requirement: CI quality gate
A GitHub Actions pipeline SHALL run on pull requests and on pushes to the main branch, running lint, tests with coverage and Docker image builds, and SHALL fail if any step fails.

#### Scenario: Passing pipeline
- **WHEN** lint, backend tests, frontend tests and both Docker builds all succeed
- **THEN** the pipeline reports success

#### Scenario: Coverage below baseline
- **WHEN** backend or frontend line coverage falls below 80%
- **THEN** the pipeline fails

#### Scenario: Failing test
- **WHEN** any test fails
- **THEN** the pipeline fails and does not build or publish images
