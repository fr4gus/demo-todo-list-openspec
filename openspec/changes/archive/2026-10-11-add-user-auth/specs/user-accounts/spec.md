# Spec Delta

## Purpose

Defines who can have an account, what is stored about them, how passwords are protected and how the single administrator account comes to exist.

## ADDED Requirements

### Requirement: Self-service registration
The system SHALL let a visitor create an account by providing a full name (one field), an email and a password. New accounts SHALL have the role `USER`.

#### Scenario: Successful registration
- **WHEN** a visitor submits a non-empty full name, a valid unused email and a valid password to `POST /api/v1/auth/register`
- **THEN** the system responds `201`, creates a `USER` account and returns no token and no password data

#### Scenario: Role cannot be chosen
- **WHEN** the registration request includes a `role` field
- **THEN** the field is ignored and the account is created as `USER`

#### Scenario: Invalid input
- **WHEN** the full name is blank, the email is malformed, or the password is shorter than 8 characters
- **THEN** the system responds `400` with the invalid fields listed

### Requirement: Unique email
An email address SHALL belong to at most one account, compared case-insensitively.

#### Scenario: Email already registered
- **WHEN** a visitor registers with an email that already has an account
- **THEN** the system responds `409` with code `EMAIL_IN_USE` and creates nothing

#### Scenario: Different letter case
- **WHEN** a visitor registers `Ana@Example.com` and `ana@example.com` already exists
- **THEN** the system responds `409` with code `EMAIL_IN_USE`

### Requirement: Password storage
Passwords SHALL be stored only as BCrypt hashes and SHALL never appear in responses or logs.

#### Scenario: Stored value
- **WHEN** an account is created
- **THEN** the stored password value is a BCrypt hash that differs from the submitted password

### Requirement: Administrator seeding
On startup, if `ADMIN_EMAIL`, `ADMIN_PASSWORD` and `ADMIN_FULL_NAME` are all set and no `ADMIN` account exists, the system SHALL create one `ADMIN` account from them. No endpoint SHALL create or promote an administrator.

#### Scenario: First start with admin variables
- **WHEN** the application starts with all three variables set and no admin exists
- **THEN** an `ADMIN` account is created with a BCrypt-hashed password

#### Scenario: Admin already exists
- **WHEN** the application starts and an `ADMIN` account already exists
- **THEN** no account is created or changed

#### Scenario: Variables absent
- **WHEN** none of the three variables is set
- **THEN** the application starts without creating an admin and logs a warning

#### Scenario: Partial configuration
- **WHEN** only some of the three variables are set
- **THEN** the application fails to start and names the missing variables

#### Scenario: Seed email already used by a user
- **WHEN** `ADMIN_EMAIL` matches an existing `USER` account
- **THEN** the application fails to start with a clear message and changes nothing
