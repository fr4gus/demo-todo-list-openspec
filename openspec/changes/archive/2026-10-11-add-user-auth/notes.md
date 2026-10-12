# Verification notes

## 5.1 Full flow in Compose (2026-10-11)

Stack started with `docker compose up --build -d` (backend, frontend, PostgreSQL, Redis, Mailpit), `JWT_SECRET` generated,
`ADMIN_EMAIL` / `ADMIN_PASSWORD` / `ADMIN_FULL_NAME` supplied through the shell environment. Requests went through the
frontend proxy (`http://localhost:5173/api/v1`).

| Step | Result |
| ---- | ------ |
| Register `Ana@Example.com` | `201`, no body |
| Register `ana@example.com` again | `409` `EMAIL_IN_USE` (case-insensitive) |
| Sign in, correct password | `200`, role `USER`, token expires 24 h after issue |
| Sign in, wrong password | `401` `INVALID_PASSWORD` "invalid password" |
| Sign in, unknown email | `401` `EMAIL_NOT_FOUND` "Email doesn't exist" |
| Request reset, known and unknown email | both `202` with the identical body; Mailpit received exactly one email (to Ana) |
| Reset link from the email | `http://localhost:5173/reset-password?token=...` served the SPA (`200`) |
| Confirm with a 5-character password | `400`, token still usable |
| Confirm with a valid password | `204` |
| Reuse the same token | `400` `RESET_TOKEN_INVALID` |
| Sign in with the old / new password | `401` `INVALID_PASSWORD` / `200` |
| Seeded admin sign-in | `200`, role `ADMIN` |
| Protected path without a token | `401` |
| Six reset requests for one email in an hour | `202` x5, then `429` |
| Backend logs searched for the passwords and the reset token | no matches |

The reset *screen* itself (form, invalid-link state, redirect with success message) is covered by the React Testing
Library tests; this run verified the emailed link reaches the frontend route.

## Findings worth knowing

- The log-capture test (2.4) found that record `toString()` methods printed passwords and the login token at DEBUG. The
  request/response DTOs now redact them, `User.toString()` omits the hash, and `logback-spring.xml` pins Hibernate's
  entity/bind loggers off and the PostgreSQL driver at INFO.
- The guarantee holds up to DEBUG. At root `TRACE`, JDBC internals (the driver's statement `toString()`) print bound
  values, so TRACE must not be enabled in production.
- `/admin/users` is a placeholder page so role redirects can be tested; `add-admin-user-overview` replaces it.
