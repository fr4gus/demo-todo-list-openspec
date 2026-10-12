# Proposal

## Why

Each person needs their own account so lists stay separate, and the admin role needs a trustworthy identity. Sign-up, sign-in, password recovery and role-aware route protection gate every other feature.

## What Changes

- Backend: user table and entity (full name, unique email, BCrypt password hash, role `USER` or `ADMIN`), registration, login issuing one stateless JWT (24-hour lifetime, role claim), JWT validation filter and role-based authorization.
- Backend: full password recovery: request endpoint, single-use short-lived reset token (only a hash stored), email with a link to the frontend, confirm endpoint, request rate limiting.
- Backend: seed the first admin account at startup from environment variables; sign-up can never create an admin.
- Frontend: sign-in, sign-up, "forgot password" and "set new password" screens; auth slice holding the token; RTK Query auth endpoints; role-aware `ProtectedRoute`; client-side logout; automatic sign-out on a 401.
- Exact user-facing messages from the product spec: "invalid password", "Email doesn't exist", and an "email already in use" error on sign-up.

## Capabilities

### New Capabilities
- `user-accounts`: Registration, uniqueness of email, password storage rules, roles, and admin seeding.
- `authentication`: Sign-in, JWT issuance and validation, session expiry, logout, and route protection by authentication state and role.
- `password-recovery`: Requesting and completing a password reset by email.

### Modified Capabilities
<!-- None: no specs exist yet. -->

## Impact

- Depends on `add-platform-scaffold` (problem-details errors, CORS, mail catcher, base API slice, `ProtectedRoute` shell).
- New tables: `users`, `password_reset_tokens` (Flyway migration).
- New environment variables: `JWT_SECRET`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `ADMIN_FULL_NAME`, `MAIL_FROM`.
- New endpoints under `/api/v1/auth`.
- Known gap, accepted: logout is client-side and a password reset does not revoke tokens already issued; both end within 24 hours.
