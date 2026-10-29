# Spec Delta

## Purpose

Defines the behavior shared by every HTTP endpoint the backend exposes: where endpoints live, how errors are reported, how requests are logged, which origins may call the API, and how a caller checks that the service is up. Later capabilities rely on this contract instead of defining their own.

## ADDED Requirements

### Requirement: API base path

All endpoints of the application API SHALL be served under the path prefix `/api/v1`.

#### Scenario: A health check is reachable under the base path

- **WHEN** a client requests `GET /api/v1/health`
- **THEN** the service responds with HTTP 200

### Requirement: Machine-readable error contract

The API SHALL report every error as an RFC 7807 `application/problem+json` body, and every such body SHALL carry a `code` member holding a stable, machine-readable identifier for the specific failure.

#### Scenario: A domain error carries a discriminating code

- **WHEN** a request fails because of a domain rule rather than a transport fault
- **THEN** the response body is `application/problem+json` with `type`, `title`, `status` and `detail` members
- **AND** the body has a `code` member identifying the specific failure

#### Scenario: The code discriminates failures that share a status

- **WHEN** two different failures both produce HTTP 401
- **THEN** the two responses carry different `code` values

#### Scenario: The code is stable across releases

- **WHEN** a client is written against a `code` value
- **THEN** the same `code` continues to identify the same failure in later releases

### Requirement: Unhandled faults do not leak internals

The service SHALL convert an unhandled exception into a problem-details response with HTTP status 500, and SHALL NOT include the exception type, message or stack trace in the response body.

#### Scenario: An unexpected exception is contained

- **WHEN** an unhandled exception occurs while serving a request
- **THEN** the response is HTTP 500 with an `application/problem+json` body
- **AND** the body does not contain the exception class name or its message

### Requirement: Request logging is structured

The service SHALL emit exactly one structured log record per served request, containing at least the HTTP method, the request path, the response status and the elapsed time.

#### Scenario: A log record is machine-readable

- **WHEN** a request completes
- **THEN** the service emits one log record in JSON form containing the method, path, status and duration

#### Scenario: One request produces one record

- **WHEN** a single request is served
- **THEN** exactly one request log record is emitted for it

### Requirement: Single-origin CORS policy

The service SHALL accept cross-origin requests only from the configured frontend origin, and SHALL NOT grant access to any other origin.

#### Scenario: The configured origin is allowed

- **WHEN** a request carries an `Origin` header equal to the configured frontend origin
- **THEN** the response includes an `Access-Control-Allow-Origin` header naming that origin

#### Scenario: An unconfigured origin is not granted access

- **WHEN** a preflight request carries an `Origin` header for any other origin
- **THEN** the response does not include an `Access-Control-Allow-Origin` header for that origin

#### Scenario: Credentialed cross-origin requests are refused

- **WHEN** a request asks for credentialed access from an origin that is not the configured frontend origin
- **THEN** the service does not grant that access

### Requirement: Health check

The service SHALL expose a health endpoint that reports whether it is able to serve requests, and SHALL NOT require authentication to reach it.

#### Scenario: An unauthenticated caller checks health

- **WHEN** a client requests `GET /api/v1/health` without a token
- **THEN** the service responds with HTTP 200 rather than 401
