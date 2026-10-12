# Spec Delta

## Purpose

Lets a user who forgot their password regain access through a time-limited link sent to their email, without revealing more about registered accounts than necessary.

## ADDED Requirements

### Requirement: Request a password reset
The system SHALL accept a reset request for an email at `POST /api/v1/auth/password-reset/request` and respond identically whether or not an account exists.

#### Scenario: Registered email
- **WHEN** a visitor requests a reset for a registered email
- **THEN** the system responds `202`, creates a reset token and emails a link to the web app's reset screen containing the token

#### Scenario: Unknown email
- **WHEN** a visitor requests a reset for an email with no account
- **THEN** the system responds `202` with the same body, sends no email and stores no token

#### Scenario: Rate limit
- **WHEN** more than 5 reset requests are made for the same email within one hour
- **THEN** further requests respond `429` and send no email

### Requirement: Reset token properties
A reset token SHALL be random, valid for 60 minutes, usable once, and stored only as a hash.

#### Scenario: Token expiry
- **WHEN** a token older than 60 minutes is submitted
- **THEN** the reset is rejected with `400` and code `RESET_TOKEN_INVALID`

#### Scenario: Token reuse
- **WHEN** a token that has already completed a reset is submitted again
- **THEN** the reset is rejected with `400` and code `RESET_TOKEN_INVALID`

#### Scenario: Newer request supersedes
- **WHEN** a second reset is requested for the same account
- **THEN** earlier unused tokens for that account stop working

### Requirement: Complete a password reset
The system SHALL set a new password when given a valid token and a valid new password at `POST /api/v1/auth/password-reset/confirm`.

#### Scenario: Successful reset
- **WHEN** a valid token and a password of at least 8 characters are submitted
- **THEN** the system responds `204`, stores the new BCrypt hash, consumes the token, and the user can sign in with the new password and not the old one

#### Scenario: Weak new password
- **WHEN** the new password is shorter than 8 characters
- **THEN** the system responds `400` and the token remains usable

### Requirement: Recovery screens
The web app SHALL provide a "forgot password" screen reachable from sign-in and a "set new password" screen opened from the emailed link.

#### Scenario: Request confirmation message
- **WHEN** the visitor submits an email on the forgot-password screen
- **THEN** the screen shows a neutral message that a link was sent if the account exists

#### Scenario: Reset success
- **WHEN** the new password is accepted
- **THEN** the app sends the user to the sign-in screen with a success message

#### Scenario: Invalid link
- **WHEN** the token is invalid or expired
- **THEN** the screen explains the link no longer works and offers to request a new one
