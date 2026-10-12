# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Current state

Pre-implementation. The repo contains no application code, build files, or tests yet — only the product/tech spec and OpenSpec workflow scaffolding. There are no build/lint/test commands to run until the frontend and backend are scaffolded.

- `project_description.md` — the source of truth for what to build: product description, tech stack, architectural/compliance requirements, testing requirements, and screen flows. Read it before proposing or implementing anything.
- `openspec/` — spec-driven workflow (`schema: spec-driven` in `openspec/config.yaml`). `openspec/specs/` holds main specs, `openspec/changes/` holds in-flight changes, `openspec/changes/archive/` holds completed ones. All are currently empty.
- `.claude/skills/openspec-*` and `.claude/commands/opsx/*` — OpenSpec skills/slash commands: `/opsx:explore`, `/opsx:propose`, `/opsx:apply`, `/opsx:update`, `/opsx:sync`, `/opsx:archive`. New work should go through this propose → apply → archive flow rather than ad-hoc edits.

## Planned system (from `project_description.md`)

Multi-user TODO web app. Each user has a separate list; completing an item removes it from their active view (shown grayed/struck-through at the end of the list, with an Undo toast and long-press-to-restore). Items have an internal id (never shown), text, and priority (high / medium-high / medium-low / low → red / orange / green / blue). Users have a single full-name field, unique email, and password.

- **Frontend:** React + Vite (TypeScript), Redux Toolkit + RTK Query (base API slice injecting the JWT header), React Router v7 with a `ProtectedRoute` wrapper, Tailwind CSS v4 via the Vite plugin. Folder structure separates components, features, hooks, routing, and the store. Tests: Vitest, React Testing Library, MSW.
- **Backend:** Spring Boot 3.x on Java 21, layered Controller → Service → Repository → Entity. PostgreSQL via Spring Data JPA with Liquibase or Flyway migrations; stateless JWT auth with Spring Security (RBAC); BCrypt passwords; CORS restricted to the frontend origin; Redis caching (`@Cacheable`/`@CacheEvict`) on read-heavy endpoints; `@ControllerAdvice` mapping exceptions to RFC 7807 problem details; JSON structured logging (Logback/SLF4J). Tables need indexes, foreign keys, and `created_at`/`updated_at`.
- **Backend tests:** JUnit 5 + Mockito + AssertJ; `@SpringBootTest` + `@AutoConfigureMockMvc` for authenticated endpoints; Testcontainers with real PostgreSQL (no H2); 80% coverage baseline; tests must be isolated.
- **DevOps:** multi-stage Dockerfiles, Docker Compose linking frontend/backend/Postgres/Redis, CI pipeline (GitHub Actions or GitLab CI — left unspecified) for lint/test/image build.

## Specified UX behaviors worth preserving exactly

- Sign-in error messages: `"invalid password"` when the password doesn't match, `"Email doesn't exist"` when the email is unknown. Duplicate sign-up shows that the email is already in use; successful sign-up returns to the login screen. Password recovery is also expected on the sign-in screen.
- Empty list text: `"No tasks to do this time!"`; FAB label: `"Create TODO"`. Completion toast: `"Task Completed - Undo"`.
- Create TODO is a dialog opened from the FAB; the list is ordered by priority with completed items last.

Note: the spec's section numbering skips §3, and the Redis section contains a stray bullet-character line — treat these as typos, not missing requirements (confirm with the user if unsure).
