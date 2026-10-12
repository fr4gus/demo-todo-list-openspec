# Proposal

## Why

An administrator needs to keep an eye on who uses the app and how much, without being able to read anyone's tasks. This is a deliberate privacy boundary: user details and counts are visible; task content never is.

## What Changes

- Backend: `GET /api/v1/admin/users` (admin only) returning, for each `USER` account, full name, email, sign-up date and counts of active, completed and total tasks. Counts come from aggregate queries; no task text or priority is read or returned.
- Frontend: admin-only screen at `/admin/users` listing those users and counts, with a sign-out control. Read-only.
- Tests that pin the privacy boundary: admins get 403 on task endpoints, and no admin response contains task content.

## Capabilities

### New Capabilities
- `admin-user-overview`: The admin-only, read-only list of users with task counts, and the guarantee that task content is never exposed to admins.

### Modified Capabilities
<!-- None: no specs exist yet. -->

## Impact

- Depends on `add-user-auth` (`ADMIN` role, route protection) and `add-todo-items` (`todo_items` table to count).
- New backend controller, service and aggregate repository query; new frontend feature folder `features/admin/`.
- Out of scope: creating, disabling, deleting or promoting users, viewing or exporting task content, search, and per-day statistics.
