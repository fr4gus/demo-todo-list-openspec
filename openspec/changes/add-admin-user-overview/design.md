# Design

## Context

The `ADMIN` role, JWT filter and role-aware `ProtectedRoute` come from `add-user-auth`; `todo_items` and the rule that `/api/v1/todos` is `USER`-only come from `add-todo-items`. Behavior is in `specs/admin-user-overview/spec.md`.

## Goals / Non-Goals

**Goals:**
- A query shape that cannot leak task content even by accident.
- Accurate counts without caching complexity.

**Non-Goals:**
- User management actions, search, filtering, export, charts.
- Caching admin results (low traffic).

## Decisions

**Query returns only counts.** One repository query, a JPQL/native projection joining `users` to `todo_items` with `COUNT(*) FILTER (WHERE completed)` and `FILTER (WHERE NOT completed)`, grouped by user, filtered to `role = 'USER'`. The projection interface has no task fields, so task text and priority never enter the admin code path. A `LEFT JOIN` keeps users with no tasks and counts them as 0. Alternative: load tasks and count in Java; rejected because it would pull content into memory and scale poorly.

**Pagination.** Spring `Pageable` with `size` capped at 100 via `PageableHandlerMethodArgumentResolver.setMaxPageSize`, default 20, sort fixed server-side to `full_name, email` (client sort parameters ignored). The count query is a separate simple count of `USER` accounts.

**Security.** Controller secured with `hasRole('ADMIN')`; no shared code with the todo controller. A dedicated `AdminUserSummary` DTO makes the contract explicit.

**No caching.** Counts must follow changes immediately, admin traffic is low, and cache invalidation across user and todo mutations would couple the two changes.

**Frontend.** `features/admin/`: `adminApi.getUsers({page,size})`, `AdminUsersPage` with a table and prev/next paging, mounted at `/admin/users` under `ProtectedRoute role="ADMIN"`, reusing the sign-out control from the auth change. No row interaction.

**Privacy test.** A MockMvc test seeds users with distinctive task text and asserts the admin response body does not contain that text, plus a test that an admin token gets 403 on every `/api/v1/todos` endpoint.

## Risks / Trade-offs

- [Counting with a join scans todo rows per page] → Indexed on `user_id`; fine at expected scale; revisit with materialized counts if it grows.
- [A future change could add a field that exposes content] → The privacy tests fail if task text, priority or id appears in admin responses.
- [Unlisted admin accounts cannot be seen in the UI] → Intentional; admins are seeded, not managed.
