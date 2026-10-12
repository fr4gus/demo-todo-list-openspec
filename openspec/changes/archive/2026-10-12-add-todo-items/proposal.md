# Proposal

## Why

The product is a per-user TODO list. The backend needs to store items, keep each user's list private, and serve it in the order the screens expect, including completed items that stay in the list.

## What Changes

- Backend: `todo_items` table and entity (internal id, owner, text, priority, completed flag, completed time, audit fields).
- Endpoints under `/api/v1/todos`: list, create, edit (text and priority), delete, and set completion (complete or restore).
- Ordering rule applied by the server: active items before completed ones, then priority high to low, then oldest first.
- Ownership enforced on every operation; other users' items behave as non-existent.
- Redis caching of each user's list with eviction on every change.
- Completed items are kept (not deleted) so they can be restored; deleting is a separate, permanent action.

## Capabilities

### New Capabilities
- `todo-items`: Creating, listing, editing, completing, restoring and deleting a user's own TODO items, including ordering, validation, privacy and caching behavior.

### Modified Capabilities
<!-- None: no specs exist yet. -->

## Impact

- Depends on `add-platform-scaffold` and `add-user-auth` (current-user identity, `USER` role, problem-details errors, Redis cache manager).
- New table `todo_items` with an index on `(user_id, completed, priority, created_at)`, foreign key to `users`.
- New Redis cache `todos`, keyed by user id.
- API consumed by `add-todo-ui` and, for counts only, by `add-admin-user-overview`.
