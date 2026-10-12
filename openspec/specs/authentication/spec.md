# authentication Specification

## Purpose

Defines how users prove who they are, how long a session lasts, and how the app restricts routes and endpoints by authentication state and role.

## Requirements

### Requirement: Sign-in with email and password
The system SHALL authenticate a user with email and password at `POST /api/v1/auth/login` and, on success, return a signed JWT access token, its expiry time and the user's id, full name, email and role.

#### Scenario: Successful sign-in
- **WHEN** a user submits a registered email and the matching password
- **THEN** the system responds `200` with a token that expires 24 hours after issue and the user's profile

#### Scenario: Unknown email
- **WHEN** the submitted email has no account
- **THEN** the system responds `401` with code `EMAIL_NOT_FOUND` and detail "Email doesn't exist"

#### Scenario: Wrong password
- **WHEN** the email exists and the password does not match
- **THEN** the system responds `401` with code `INVALID_PASSWORD` and detail "invalid password"

### Requirement: Stateless token validation
Protected endpoints SHALL require `Authorization: Bearer <token>` and SHALL accept only tokens that are correctly signed, unexpired and issued to an existing account. The token SHALL carry the user's id and role.

#### Scenario: Valid token
- **WHEN** a request carries a valid unexpired token
- **THEN** the request is processed as that user

#### Scenario: Expired or tampered token
- **WHEN** a request carries an expired token or one with an invalid signature
- **THEN** the system responds `401`

#### Scenario: No token
- **WHEN** a protected endpoint is called without a token
- **THEN** the system responds `401`

### Requirement: Role-based endpoint access
The system SHALL restrict endpoints by role: `/api/v1/auth/**` is public, and other endpoints require the role their capability specifies.

#### Scenario: Wrong role
- **WHEN** an authenticated caller lacks the role an endpoint requires
- **THEN** the system responds `403`

### Requirement: Client session handling
The web app SHALL keep the token client-side, send it with API requests, and treat a missing or expired token as signed out.

#### Scenario: Sign-in destination for a user
- **WHEN** a `USER` signs in successfully
- **THEN** the app shows the main TODO list screen

#### Scenario: Sign-in destination for an admin
- **WHEN** an `ADMIN` signs in successfully
- **THEN** the app shows the admin user overview

#### Scenario: Reload keeps the session
- **WHEN** a signed-in user reloads the page before the token expires
- **THEN** the user remains signed in

#### Scenario: Token rejected
- **WHEN** any API call returns `401`
- **THEN** the app discards the token and shows the sign-in screen

#### Scenario: Logout
- **WHEN** the user chooses to sign out
- **THEN** the app discards the token and shows the sign-in screen

### Requirement: Route protection
The app SHALL show protected screens only to signed-in users with the required role. The sign-in, sign-up, forgot-password and reset-password screens SHALL be public.

#### Scenario: Visitor opens a protected route
- **WHEN** a signed-out visitor opens `/`
- **THEN** the app shows the sign-in screen

#### Scenario: User opens an admin route
- **WHEN** a `USER` opens `/admin/users`
- **THEN** the app redirects to `/`

#### Scenario: Admin opens the user route
- **WHEN** an `ADMIN` opens `/`
- **THEN** the app redirects to `/admin/users`

### Requirement: Sign-in and sign-up screens
The first screen SHALL be sign-in (email, password) with links to sign-up and password recovery. Sign-up SHALL collect full name, email and password.

#### Scenario: Sign-in errors are shown
- **WHEN** sign-in fails with `EMAIL_NOT_FOUND` or `INVALID_PASSWORD`
- **THEN** the screen shows "Email doesn't exist" or "invalid password" respectively

#### Scenario: Duplicate email on sign-up
- **WHEN** sign-up fails with `EMAIL_IN_USE`
- **THEN** the screen shows that the email is already in use and stays on the form

#### Scenario: Successful sign-up
- **WHEN** sign-up succeeds
- **THEN** the app sends the user back to the sign-in screen
