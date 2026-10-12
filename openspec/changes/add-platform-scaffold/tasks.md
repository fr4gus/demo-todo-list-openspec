# Tasks

## 1. Repository layout

- [ ] 1.1 Create `backend/`, `frontend/`, `.github/workflows/` and a root `.gitignore` (build outputs, `.env`, IDE files); verify `git status` shows no ignored artifacts after a build
- [ ] 1.2 Add `.env.example` documenting `FRONTEND_ORIGIN`, `JWT_SECRET`, `POSTGRES_*`, `REDIS_*`, `SMTP_*` with empty or fake values; verify every variable referenced by compose and `application.yml` appears in it

## 2. Backend foundation

- [ ] 2.1 Generate the Maven project (Spring Boot 3.x, Java 21) with web, validation, security, data-jpa, data-redis, mail, actuator-free health controller, Flyway, PostgreSQL driver, logstash-logback-encoder, jjwt, test deps; verify `mvn -q verify` runs on an empty test suite
- [ ] 2.2 Add layered package structure and the `/api/v1/health` controller; verify with a MockMvc test that `GET /api/v1/health` returns 200
- [ ] 2.3 Add the base audited entity, an initial Flyway baseline migration and JPA auditing; verify a Testcontainers repository test shows `created_at` is stable and `updated_at` advances on update
- [ ] 2.4 Add `@RestControllerAdvice` problem-details handling with `code` extension, plus 401/403 handlers in the security config; verify MockMvc tests for 400 validation, 404, 500 (no stack trace) and 401/403 bodies
- [ ] 2.5 Add JSON logging with correlation-id filter; verify a test captures log output as valid JSON with the correlation id and that an `Authorization` header value never appears
- [ ] 2.6 Add the CORS configuration from `FRONTEND_ORIGIN`; verify MockMvc tests for the allowed origin and for a rejected foreign origin
- [ ] 2.7 Add the Redis cache manager configuration (TTL, JSON serializer); verify an integration test using a Redis Testcontainer stores and evicts a sample cache entry
- [ ] 2.8 Add startup validation for required properties such as `JWT_SECRET`; verify the context fails with a message naming the missing variable
- [ ] 2.9 Configure JaCoCo with the 80% line-coverage check; verify `mvn verify` fails when the threshold is raised above actual coverage

## 3. Frontend foundation

- [ ] 3.1 Scaffold Vite + React + TypeScript with the folder structure (`app`, `components`, `features`, `hooks`, `routing`, `store`), ESLint and Prettier; verify `npm run lint` and `npm run build` pass
- [ ] 3.2 Add Tailwind CSS v4 through the Vite plugin; verify a utility class renders styled in `npm run dev`
- [ ] 3.3 Add the Redux store, typed hooks and the base RTK Query API slice with Bearer-token header injection; verify a unit test shows the header is sent when a token exists and omitted otherwise
- [ ] 3.4 Add React Router v7 with a `ProtectedRoute` shell reading an auth selector; verify a test redirects an unauthenticated visitor to `/login`
- [ ] 3.5 Set up Vitest, React Testing Library and MSW with per-test handler reset; verify a sample test dispatches a Redux action from a component and asserts the state change
- [ ] 3.6 Add a sample MSW integration test with one success and one failure response from the backend; verify both cases are asserted in the UI
- [ ] 3.7 Configure Vitest coverage with an 80% line threshold; verify `npm run test:coverage` fails when the threshold is raised above actual coverage

## 4. Containers and Compose

- [ ] 4.1 Write the backend multi-stage Dockerfile (`dev`, `build`, `prod`; non-root, JRE-only prod); verify `docker build --target prod` succeeds and the container runs as a non-root user
- [ ] 4.2 Write the frontend multi-stage Dockerfile (`dev` with Vite, `prod` served by nginx); verify both targets build
- [ ] 4.3 Write `docker-compose.yml` with frontend, backend, postgres, redis, mailpit, healthchecks and `depends_on` health conditions; verify `docker compose config` is valid and `docker compose up --build` reaches a healthy `/api/v1/health`
- [ ] 4.4 Write the execution guide in `README.md` (prerequisites, `.env` setup, up/down, running tests, ports); verify the documented commands work from a clean clone

## 5. CI pipeline

- [ ] 5.1 Add `.github/workflows/ci.yml` with `backend`, `frontend` and `docker` jobs triggered on pull requests and pushes to main; verify with `actionlint` or a pushed branch that the workflow parses and runs
- [ ] 5.2 Make the `docker` job depend on the test jobs; verify a deliberately failing test stops the image build
