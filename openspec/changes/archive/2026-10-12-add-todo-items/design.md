# Design

## Context

Requires the user identity and `USER`/`ADMIN` roles from `add-user-auth`, and the Redis cache manager and problem-details handling from `add-platform-scaffold`. Behavior is in `specs/todo-items/spec.md`.

## Goals / Non-Goals

**Goals:**
- Simple, ownership-safe CRUD plus completion toggle.
- Ordering decided by the server so every client shows the same order.
- Cache that is simple enough to reason about.

**Non-Goals:**
- Pagination of the user list (lists are small; revisit if it grows, since completed items are never purged).
- Due dates, tags, sharing, reordering by drag and drop, bulk operations.
- Soft delete or an audit trail of deletions.

## Decisions

**Table `todo_items`.** `id` UUID PK, `user_id` FK -> `users` (`ON DELETE CASCADE`), `text` varchar(500), `priority` smallint (0 high ... 3 low) with a check constraint, `completed` boolean default false, `completed_at` timestamptz null, audit columns. Index `(user_id, completed, priority, created_at)` matches the list ordering. Storing priority as a number makes `ORDER BY` correct without a CASE; the API exposes names via an enum converter.

**Ordering in SQL.** `ORDER BY completed, priority, created_at, id` (`id` as a stable tiebreaker). Alternative: sort in the frontend; rejected so the contract is server-defined.

**Ownership.** All repository methods take `userId` (`findByIdAndUserId`, `deleteByIdAndUserId`). A missing result throws `NotFoundException` (404), which covers both "no such id" and "someone else's id" so ids cannot be probed. The controller reads the user id from the authenticated principal; there is no user id in any path or body.

**Roles.** The controller is annotated for `hasRole('USER')`, which makes admin calls a 403 without any item-specific code. This is also what keeps task content away from admins.

**API.** `PUT` replaces text and priority (both required) rather than `PATCH`, since the edit dialog always submits both. Completion is a separate `PATCH .../completion` so Undo, long-press restore and "Mark as incomplete" all use one idempotent call. Responses use a `TodoResponse` DTO (id, text, priority, completed, createdAt); entities are never returned.

**Caching.** `@Cacheable(cacheNames = "todos", key = "#userId")` on the service method that returns the ordered list; `@CacheEvict(cacheNames = "todos", key = "#userId")` on create, update, set-completion and delete. Eviction happens after the transaction commits (cache annotations are applied around the transactional method with the default advice order, and tests verify read-after-write). Alternative: no cache; the spec requires caching on read-heavy endpoints and the list endpoint is the one. TTL stays at the default 10 minutes as a safety net.

**Validation.** Bean Validation on the request records (`@NotBlank`, `@Size(max=500)`, `@NotNull` priority); text is trimmed in the service before saving.

## Risks / Trade-offs

- [Unbounded list growth because completed items are never purged] → Accepted per product decision; delete is available; add pagination or archiving in a later change if needed.
- [Cache/transaction ordering could serve stale data] → Integration test with real Redis and PostgreSQL for write-then-read; short TTL as backstop.
- [404 for foreign ids hides authorization failures from logs] → Log a warning with user id and item id (no content) on foreign access.
- [Whole-list cache entries are rewritten on every change] → Fine at this scale.
