# Tasks

## 1. Workspace layout

- [ ] 1.1 Create the `backend/`, `frontend/` and `.github/workflows/` directories and a root `docker-compose.yml` placeholder, and verify the tree matches the blueprint in the README
- [ ] 1.2 Check in the Maven wrapper (`mvnw`, `mvnw.cmd`, `.mvn/wrapper/`) and verify `./mvnw -v` reports Java 21 without a system Maven installed
- [ ] 1.3 Initialise the frontend with the Vite React + TypeScript template, and verify `npm install && npm run build` succeeds

## 2. Backend foundation

- [ ] 2.1 Create `backend/pom.xml` with the Spring Boot 3, Spring Data JPA, Flyway, Spring Data Redis, Spring Mail, jjwt, PostgreSQL and actuator dependencies, and verify `./mvnw -B compile` succeeds
- [ ] 2.2 Add the layered package structure (`controller`, `service`, `repository`, `entity`, `dto`, `config`, `common`) and verify the application starts with `./mvnw spring-boot:run` and logs a startup banner
- [ ] 2.3 Implement the `ProblemDetail` exception handler with the `code` property, and verify a test asserts that two failures sharing HTTP 401 return different `code` values
- [ ] 2.4 Implement the unhandled-exception handler, and verify a test asserts that a thrown runtime exception yields HTTP 500 with no exception class name or message in the body
- [ ] 2.5 Configure structured JSON logging with a per-request record containing method, path, status and duration, and verify by capturing the log output that one request produces exactly one JSON record with those four fields
- [ ] 2.6 Implement the CORS configuration reading `FRONTEND_ORIGIN`, and verify a test asserts the configured origin is allowed, another origin is not, and credentialed access from another origin is refused
- [ ] 2.7 Implement the health endpoint, and verify a test asserts `GET /api/v1/health` returns 200 without a token
- [ ] 2.8 Wire Flyway with an initial migration creating the audit-column convention, and verify a Testcontainers test against real PostgreSQL sees the migrated schema
- [ ] 2.9 Configure the Redis cache manager with per-cache TTL and JSON serialization, and verify a test asserts the `todos` cache has a 60-second TTL and that a cached value round-trips as JSON
- [ ] 2.10 Write the multi-stage backend Dockerfile with `dev` and `prod` targets, and verify `docker build --target prod` succeeds and the resulting image serves `GET /api/v1/health`

## 3. Frontend foundation

- [ ] 3.1 Create the `src/` folder structure (`app`, `components`, `features`, `hooks`, `routing`, `store`, `lib`) and verify each directory exists in the tree
- [ ] 3.2 Configure the Redux store with typed hooks and the base RTK Query API slice, and verify a unit test asserts the slice is mounted and its reducer is reachable
- [ ] 3.3 Implement the `prepareHeaders` callback that attaches a bearer token from the store, and verify a unit test asserts the `Authorization` header is present when a token is set and absent when it is not
- [ ] 3.4 Set up React Router v7 with a placeholder route and a 404 fallback, and verify a test renders the router and shows the placeholder route
- [ ] 3.5 Install Tailwind CSS v4 through the Vite plugin and verify a utility class compiles to CSS in the production build
- [ ] 3.6 Set up Vitest, React Testing Library and MSW with a shared `server` and a default handler set, and verify a test renders a component and an MSW handler intercepts its request

## 4. Test infrastructure and coverage

- [ ] 4.1 Configure JUnit 5, Mockito and AssertJ with a reusable Testcontainers PostgreSQL container, and verify a repository test runs against the real database and passes
- [ ] 4.2 Configure JaCoCo to produce a report and enforce a hard 80% line-coverage check, excluding configuration classes, entities, DTOs and the application entry point, and verify `./mvnw -B verify` fails when a deliberately uncovered class is introduced and passes when it is removed
- [ ] 4.3 Configure Vitest coverage with a hard 80% threshold on lines, functions, branches and statements, excluding entry points, type declarations and test setup files, and verify `npm run test:coverage` fails when a deliberately uncovered file is added and passes when it is removed
- [ ] 4.4 Verify that both coverage configurations exclude configuration and boilerplate files from the measured percentage, and confirm the exclusions are listed in a test or a documented configuration file

## 5. Local stack and documentation

- [ ] 5.1 Write `docker-compose.yml` with the frontend, backend, PostgreSQL, Redis and mail catcher services, and verify `docker compose up` brings all five to a reachable state
- [ ] 5.2 Add `.env.example` listing every variable the stack needs, and verify the stack starts from a copy of it without further edits
- [ ] 5.3 Write the README with prerequisites and tool versions, and verify each stated version matches the versions the project actually pins
- [ ] 5.4 Add the directory tree blueprint to the README, and verify it matches the real tree produced by the previous groups
- [ ] 5.5 Document the single command that starts the stack, the URL of the running application and the URL of the mail catcher's web interface, and verify each command and URL works as written
- [ ] 5.6 Document the known limitations of the local stack, including that the dev image differs from the image CI builds, and verify the limitations section names each difference

## 6. Continuous integration

- [ ] 6.1 Add the `backend` CI job running `./mvnw -B verify`, and verify it runs on a pull request and reports a failure when a backend test fails
- [ ] 6.2 Add the `frontend` CI job running lint, coverage tests and a production build, and verify it reports a failure when a lint error or a failing test is introduced
- [ ] 6.3 Add the `docker` CI job building both `prod` images, and verify it reports a failure when a Dockerfile does not build
- [ ] 6.4 Add the coverage-gate enforcement to the pipeline so that a drop below 80% in either project fails the run, and verify the failure is reported against the responsible project
