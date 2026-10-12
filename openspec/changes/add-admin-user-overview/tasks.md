# Tasks

## 1. Backend

- [ ] 1.1 Add the aggregate repository query and `AdminUserSummary` projection (users with active/completed/total counts, `USER` role only, ordered by name then email); verify a Testcontainers test with users having zero, some active and some completed tasks, and that admin accounts are excluded
- [ ] 1.2 Add `AdminUserController` at `GET /api/v1/admin/users` with pagination defaults (20) and a cap (100), restricted to `ADMIN`; verify MockMvc tests for 200 as admin, 403 as user, 401 without token, paging metadata and the size cap
- [ ] 1.3 Add privacy tests: seed tasks with distinctive text and verify the admin response never contains that text, priority or a task id, and that an admin token receives 403 on every `/api/v1/todos` endpoint
- [ ] 1.4 Add a test that counts follow task mutations (create, complete, restore, delete) on the next request, confirming nothing is cached

## 2. Frontend

- [ ] 2.1 Add `adminApi.getUsers` to the base API; verify an MSW test for the request parameters and the response mapping
- [ ] 2.2 Build `AdminUsersPage` with the table, paging, empty message and sign-out control; verify RTL + MSW tests for table content, empty state, next/previous page and absence of any task view or user actions
- [ ] 2.3 Mount `/admin/users` under the admin-only `ProtectedRoute`; verify tests that a `USER` is redirected to `/` and a signed-out visitor to sign-in

## 3. Integration

- [ ] 3.1 In the Compose stack, sign in as the seeded admin, check counts against two test users' lists, and confirm no task text is visible anywhere; verify manually and note the result
