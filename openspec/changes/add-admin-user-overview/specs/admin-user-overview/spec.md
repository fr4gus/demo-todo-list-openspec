# Spec Delta

## Purpose

Gives administrators a read-only view of registered users and how many tasks each has, while guaranteeing that the content of tasks stays private to their owners.

## ADDED Requirements

### Requirement: Admin-only user list
`GET /api/v1/admin/users` SHALL return the registered `USER` accounts to callers with the `ADMIN` role, and SHALL be forbidden to everyone else.

#### Scenario: Admin request
- **WHEN** an `ADMIN` calls the endpoint
- **THEN** the system responds `200` with a page of users

#### Scenario: Regular user
- **WHEN** a `USER` calls the endpoint
- **THEN** the system responds `403`

#### Scenario: Not signed in
- **WHEN** the request has no valid token
- **THEN** the system responds `401`

### Requirement: User details and task counts
Each entry SHALL contain the user's id, full name, email, registration date, and the numbers of active, completed and total tasks. The total SHALL equal active plus completed.

#### Scenario: Counts
- **WHEN** a user has 3 active and 2 completed tasks
- **THEN** their entry shows active 3, completed 2, total 5

#### Scenario: No tasks
- **WHEN** a user has no tasks
- **THEN** their entry shows 0, 0 and 0

#### Scenario: Counts follow changes
- **WHEN** a user completes, restores, creates or deletes a task
- **THEN** the next admin request shows updated counts

#### Scenario: Administrators are not listed
- **WHEN** the list is requested
- **THEN** `ADMIN` accounts do not appear

### Requirement: Task content is never exposed to administrators
No administrator-facing endpoint or screen SHALL return or display any task's text, priority or id.

#### Scenario: Response content
- **WHEN** an admin fetches the user list for users who have tasks
- **THEN** the response contains no task text, priority or task id

#### Scenario: Task endpoints
- **WHEN** an admin calls any `/api/v1/todos` endpoint
- **THEN** the system responds `403`

### Requirement: Predictable listing
The list SHALL be paginated (default 20 per page, maximum 100) and ordered by full name, then email.

#### Scenario: Paging
- **WHEN** more users exist than the page size
- **THEN** the response includes the requested page and the total number of users

#### Scenario: Oversized page request
- **WHEN** a page size above 100 is requested
- **THEN** the system caps it at 100

### Requirement: Admin screen
The web app SHALL show signed-in admins a read-only table of the users with their name, email and the three counts at `/admin/users`, with a way to sign out and to move between pages.

#### Scenario: Table content
- **WHEN** an admin opens the screen
- **THEN** each user appears with name, email, active, completed and total counts

#### Scenario: Empty system
- **WHEN** there are no users
- **THEN** the screen shows a message that no users have registered yet

#### Scenario: No task content or controls
- **WHEN** the admin screen is displayed
- **THEN** it offers no way to view tasks and no actions that change users
