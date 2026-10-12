# Spec Delta

## Purpose

Defines the behavior every REST endpoint shares: a versioned base path, a standard error format, structured logs and a strict cross-origin policy, so feature capabilities can rely on them without restating them.

## ADDED Requirements

### Requirement: Versioned API base path
All application endpoints SHALL be served under `/api/v1`.

#### Scenario: Endpoint under the base path
- **WHEN** a client calls `GET /api/v1/health`
- **THEN** the service responds `200` with a body indicating it is up

#### Scenario: Unknown path
- **WHEN** a client requests a path that does not exist
- **THEN** the service responds `404` using the standard error format

### Requirement: Standard error format
Every error response SHALL be an RFC 7807 problem-details document (`application/problem+json`) with `type`, `title`, `status` and `detail`, plus a machine-readable `code` extension for known application errors.

#### Scenario: Validation failure
- **WHEN** a request body fails validation
- **THEN** the service responds `400` with a problem document that lists each invalid field and its message

#### Scenario: Unexpected failure
- **WHEN** an unhandled exception occurs
- **THEN** the service responds `500` with a generic problem document that contains no stack trace or internal class names

#### Scenario: Unauthenticated request
- **WHEN** a protected endpoint is called without a valid token
- **THEN** the service responds `401` using the standard error format

#### Scenario: Forbidden request
- **WHEN** an authenticated caller lacks the role required by an endpoint
- **THEN** the service responds `403` using the standard error format

### Requirement: Structured JSON logging
The backend SHALL write logs to standard output as one JSON object per line, including timestamp, level, logger, message and a request correlation id when a request is being handled.

#### Scenario: Request log line
- **WHEN** a request is handled
- **THEN** each log line emitted during that request is valid JSON and carries the same correlation id

#### Scenario: Secrets are not logged
- **WHEN** any request containing a password, token or authorization header is handled
- **THEN** no log line contains those values

### Requirement: Single-origin CORS policy
The backend SHALL accept cross-origin browser requests only from the origin configured as `FRONTEND_ORIGIN`.

#### Scenario: Configured origin
- **WHEN** a browser request arrives with `Origin` equal to `FRONTEND_ORIGIN`
- **THEN** the response allows that origin, including the `Authorization` header

#### Scenario: Any other origin
- **WHEN** a browser request arrives from a different origin
- **THEN** the response carries no CORS allowance and the browser blocks it

### Requirement: Audit fields on persisted data
Every database table SHALL have `created_at` and `updated_at` columns that the system maintains, and SHALL use foreign keys and indexes for its relationships and lookup columns.

#### Scenario: Row creation and update
- **WHEN** a row is inserted and later updated
- **THEN** `created_at` keeps its original value and `updated_at` advances on the update
