# Tasks

## 1. Persistence

- [ ] 1.1 Add the Flyway migration for `todo_items` (FK to users with cascade, priority check, composite index) and the entity, priority enum with converter and repository methods scoped by `userId`; verify a Testcontainers repository test covers ordering by completed, priority and created time, and scoping by user
- [ ] 1.2 Verify deleting a user cascades to their items in a Testcontainers test

## 2. Service layer

- [ ] 2.1 Implement the service for list, create, update, set-completion and delete with trimming and not-found handling; verify Mockito unit tests for each operation, foreign id returning not-found, and idempotent completion
- [ ] 2.2 Add `@Cacheable`/`@CacheEvict` on the list and mutations; verify an integration test with Redis and PostgreSQL Testcontainers shows read-after-write for each mutation and isolation between two users

## 3. API

- [ ] 3.1 Implement `TodoController` endpoints and DTOs with Bean Validation, restricted to the `USER` role; verify `@SpringBootTest` + MockMvc tests for 201/200/204, 400 with field errors, 404 for another user's id, 401 without token and 403 for an admin token
- [ ] 3.2 Verify the list endpoint returns the specified order for a mixed set of items through MockMvc
- [ ] 3.3 Log foreign-id access attempts without item content; verify via a log-capture test

## 4. Documentation

- [ ] 4.1 Document the endpoints (paths, bodies, status codes, ordering rule) in `backend/README.md` or an OpenAPI description served by the app; verify every documented example matches an actual MockMvc test
